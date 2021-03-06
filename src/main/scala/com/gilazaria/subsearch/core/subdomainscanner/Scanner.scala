package com.gilazaria.subsearch.core.subdomainscanner

import com.gilazaria.subsearch.connection.{DNSLookupImpl, DNSLookup}
import com.gilazaria.subsearch.core.subdomainscanner.ScannerMessage._
import com.gilazaria.subsearch.core.subdomainscanner.DispatcherMessage.{AvailableForScan, CompletedScan, FailedScan, PriorityScanSubdomain}
import com.gilazaria.subsearch.core.subdomainscanner.ListenerMessage.{FoundSubdomain, ScanTimeout}
import com.gilazaria.subsearch.utils.TimeoutFuture._
import akka.actor.{Actor, Props, ActorRef}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object Scanner {
  def props(listener: ActorRef, hostname: String)(implicit ec: ExecutionContext): Props =
    Props(new Scanner(listener, hostname))
}

class Scanner(listener: ActorRef, hostname: String)(implicit ec: ExecutionContext) extends Actor {
  private val lookup: DNSLookup = DNSLookupImpl.create()

  override def postRestart(reason: Throwable) = {
    preStart
    // Reporting for duty after restart
    context.parent ! AvailableForScan
  }

  def receive = {
    case ScanAvailable =>
      // Notified about available work by parent (Subdomain Dispatcher)
      context.parent ! AvailableForScan

    case Scan(subdomain, resolver, attempt) =>
      val timeout =
        if (attempt == 1) 10.seconds
        else if (attempt == 2) 20.seconds
        else 30.seconds

      Future(lookup.performQueryOfTypeANY(subdomain, resolver))
        .withTimeout(timeout)
        .map(records => self ! ScanComplete(records, subdomain, resolver))
        .recover { case cause => self ! ScanFailed(cause, subdomain, resolver, attempt+1) }

    case ScanComplete(recordsAttempt, subdomain, resolver) =>
      if (recordsAttempt.isSuccess) {
        val records = recordsAttempt.get

        if (records.nonEmpty)
          listener ! FoundSubdomain(subdomain, records)

        records
          .filter(_.recordType.isOneOf("CNAME", "SRV", "MX"))
          .filter(record => record.data.endsWith(hostname))
          .map(_.data)
          .foreach((subdomain: String) => context.parent ! PriorityScanSubdomain(subdomain))
      } else {
        // Do nothing. This indicates that the DNSLookup class tried three times to lookup the subdomain.
        // For the moment, we aren't going to try again and will mark this subdomain scan as completed.
      }

      context.parent ! CompletedScan(subdomain, resolver)

    case ScanFailed(cause, subdomain, resolver, attempt) =>
      if (attempt < 4) {
        listener ! ScanTimeout(subdomain, resolver, attempt)
        self ! Scan(subdomain, resolver, attempt)
      }
      else {
        context.parent ! FailedScan(subdomain, resolver)
      }
  }
}
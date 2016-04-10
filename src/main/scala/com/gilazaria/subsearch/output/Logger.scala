package com.gilazaria.subsearch.output

import com.gilazaria.subsearch.model.Record

import scala.collection.SortedSet
import scala.concurrent.Future

trait Logger {
  def logHeader(header: String)
  def logConfig(threads: Int, wordlistSize: Int, resolverslistSize: Int)
  def logTarget(hostname: String)

  // Authoritative Scanner
  def logAuthoritativeScanStarted()
  def logAuthoritativeNameServer(nameServer: String)
  def logAuthoritativeScanCompleted()

  // Zone Transfer Scanner
  def logStartedZoneTransfer()
  def logNameServersNotVulnerableToZoneTransfer()
  def logNameServerVulnerableToZoneTransfer(nameServer: String)
  def logZoneTransferCompleted()

  // DNS Dumpster Scanner
  def logDNSDumpsterScanStarted()
  def logDNSDumpsterConnectionError(msg: String)
  def logDNSDumpsterFoundSubdomains(subdomains: Set[String])
  def logDNSDumpsterScanCompleted()

  // Controller
  def logAddingAuthNameServersToResolvers(totalResolversSize: Int)

  // Subdomain Bruteforce Scanner
  def logStartedSubdomainSearch()
  def logTaskCompleted()
  def logTaskFailed()
  def logPausingThreads()
  def logPauseOptions()
  def logInvalidPauseOption()
  def logNotEnoughResolvers()
  def logTimedOutScan(subdomain: String, resolver: String, duration: String)
  def logBlacklistedResolver(resolver: String)
  def logScanCancelled()
  def logScanForceCancelled()
  def logLastRequest(subdomain: String, numberOfRequestsSoFar: Int, totalNumberOfSubdomains: Int)

  // Misc
  def logRecords(records: SortedSet[Record])
  def logRecordsDuringScan(records: SortedSet[Record])

  // Utility
  def completedLoggingFuture: Future[Unit]
}

package com.box.l10n.mojito.service.blobstorage.database;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class DatabaseBlobStorageCleanupJob implements Job {

  static Logger logger = LoggerFactory.getLogger(DatabaseBlobStorageCleanupJob.class);

  @Autowired DatabaseBlobStorage databaseBlobStorage;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    logger.debug("Cleanup expired blobs");
    databaseBlobStorage.deleteExpired();
  }
}

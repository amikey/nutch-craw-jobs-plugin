/**
 * 
 */
package com.atlantbh.nutch.filter.xpath;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.nutch.crawl.AbstractFetchSchedule;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.DefaultFetchSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pcaparroy
 *
 */
public class MainPageFetchSchedule extends DefaultFetchSchedule {
	private static final Logger LOG = LoggerFactory.getLogger(MainPageFetchSchedule.class);
	 
	/**
	 * 
	 */
	public MainPageFetchSchedule() {
		super();
	}

	@Override
	public CrawlDatum setFetchSchedule(Text url, CrawlDatum datum,
			long prevFetchTime, long prevModifiedTime, long fetchTime,
			long modifiedTime, int state) {
		 datum.setRetriesSinceFetch(0);
		 
		return super.setFetchSchedule(url, datum, prevFetchTime, prevModifiedTime, fetchTime, modifiedTime, state);
			   
	}
	// pages are never truly GONE - we have to check them from time to time.
    // pages with too long fetchInterval are adjusted so that they fit within
    // maximum fetchInterval (segment retention period).
	@Override
	public boolean shouldFetch(Text url, CrawlDatum datum, long curTime) {
		
		
		
		boolean shouldFetch = super.shouldFetch(url, datum, curTime);
		/* Writable fetchInterval = datum.getMetaData().get(new Text("fetchInterval"));
		 
		 if(fetchInterval!=null){
			 try {
				 
				Integer datumFetchInterval = Integer.parseInt(fetchInterval.toString());
				datum.setFetchInterval(datumFetchInterval.intValue());
			 } catch (NumberFormatException e) {
				
				LOG.warn("failed to convert datum fetch interval to int");
			 }
		 }*/
		Writable refetchPage = datum.getMetaData().get(new Text("refetch"));
		
		if( ((refetchPage!=null&&shouldFetch ) || (datum.getStatus()!=CrawlDatum.STATUS_DB_FETCHED))){
			datum.setFetchTime(curTime);
			
		}else{
			if(refetchPage==null)
				datum.setFetchTime(curTime+((long)maxInterval*1000));
		}
		if(LOG.isDebugEnabled()){
			
			
		}
			
		 
	    if (datum.getFetchTime() > curTime) {
	      return false;                                   // not time yet
	    }
	    return true;
	}
	
	

}

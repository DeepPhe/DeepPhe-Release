package org.healthnlp.deepphe.summary.attribute.infostore;

import java.util.Map;

public interface CodeInfoStore {

   void init( UriInfoStore uriInfoStore, Map<String,String> dependencies );

   String getBestCode();

}

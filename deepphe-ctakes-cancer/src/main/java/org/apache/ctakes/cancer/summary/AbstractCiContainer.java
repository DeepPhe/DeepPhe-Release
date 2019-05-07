package org.apache.ctakes.cancer.summary;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2019
 */
abstract public class AbstractCiContainer implements CiContainer {

   private final long _unique_id_num;

   private final String _worldId;
   private final String _type;
   private final String _uri;

   /**
    * @param type -
    * @param uri              type uri
    * @param worldId -
    */
   public AbstractCiContainer( final String type, final String uri, final String worldId ) {
      _unique_id_num = createUniqueIdNum();
      _type = type;
      _uri = uri;
      _worldId = worldId;
   }

   abstract long createUniqueIdNum();

   /**
    * ANY summary can have a patient_site_etc id in common with another.
    * For instance, patientA and patientB have current tumors on the left breast.
    *
    * @return an index number unique to this summary
    */
   final public long getUniqueIdNum() {
      return _unique_id_num;
   }




   final public String getType() {
      return _type;
   }

   /**
    * @return the most specific uri for the summary.
    */
   final public String getUri() {
      return _uri;
   }

   final public String getWorldId() {
      return _worldId;
   }




}

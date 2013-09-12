package org.ambraproject.rhino.util;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import javax.activation.MimetypesFileTypeMap;
import java.util.Collections;
import java.util.Map;

/**
 * Utility classes for assigning a MIME "Content-Type" header based on a file extension.
 * <p/>
 * This class defers to {@link MimetypesFileTypeMap} where possible, but adds several cases. This class would best be
 * replaced with a well-maintained, general-purpose utility. But, it has the advantage of guaranteeing compatibility
 * with legacy Ambra behavior.
 */
public class ContentTypeInference {
  private ContentTypeInference() {
    throw new AssertionError("Not instantiable");
  }

  private static final MimetypesFileTypeMap BUILT_IN = new MimetypesFileTypeMap(); // unmodifiable by convention
  private static final Map<String, String> HARD_CODED = buildHardCodedMap(); // case-insensitive, unmodifiable
  private static final String BUILT_IN_DEFAULT = "application/octet-stream";

  /*
   * From a legacy list of hard-coded constants (in "pmc2obj-v3.xslt"), mostly based on PLOS use cases.
   * Omits any case that appeared in "pmc2obj-v3.xslt" but is covered by MimetypesFileTypeMap.
   */
  private static Map<String, String> buildHardCodedMap() {
    CaseInsensitiveMap map = new CaseInsensitiveMap(64);

    map.put("asf", "video/x-ms-asf");
    map.put("asx", "video/x-ms-asf");
    map.put("bmp", "image/bmp");
    map.put("bz2", "application/x-bzip");
    map.put("bzip", "application/x-bzip");
    map.put("csv", "text/comma-separated-values");
    map.put("divx", "video/x-divx");
    map.put("doc", "application/msword");
    map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    map.put("dvi", "application/x-dvi");
    map.put("eps", "application/eps");
    map.put("gz", "application/x-gzip");
    map.put("gzip", "application/x-gzip");
    map.put("icb", "application/x-molsoft-icb");
    map.put("latex", "application/x-latex");
    map.put("m4v", "video/x-m4v");
    map.put("mp2", "audio/mpeg");
    map.put("mp3", "audio/x-mpeg3");
    map.put("mp4", "video/mp4");
    map.put("mpg4", "video/mp4");
    map.put("pdf", "application/pdf");
    map.put("png", "image/png");
    map.put("pnm", "image/x-portable-anymap");
    map.put("ppt", "application/vnd.ms-powerpoint");
    map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    map.put("ra", "audio/x-realaudio");
    map.put("ram", "audio/x-pn-realaudio");
    map.put("rar", "application/x-rar-compressed");
    map.put("ras", "image/x-cmu-raster");
    map.put("rm", "audio/x-pn-realaudio");
    map.put("rmi", "audio/midi");
    map.put("rtf", "text/rtf");
    map.put("snd", "audio/basic");
    map.put("swf", "application/x-shockwave-flash");
    map.put("tar", "application/x-tar");
    map.put("wma", "audio/x-ms-wma");
    map.put("wmv", "video/x-ms-wmv");
    map.put("xls", "application/vnd.ms-excel");
    map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    map.put("xml", "text/xml");
    map.put("xpm", "image/x-xpixmap");
    map.put("zip", "application/zip");

    return Collections.unmodifiableMap(map);
  }

  private static String getFileExtension(String filename) {
    int index = filename.lastIndexOf('.');
    return (index < 0) ? null : filename.substring(index + 1);
  }

  /**
   * Infer the best "Content-Type" header from the extension of a filename.
   *
   * @param filename the full filename with an extension
   * @return the "Content-Type" header value
   */
  public static String inferContentType(String filename) {
    String mimeType = HARD_CODED.get(getFileExtension(filename));
    if (mimeType != null) {
      return mimeType;
    }
    mimeType = BUILT_IN.getContentType(filename);
    if (BUILT_IN_DEFAULT.equals(mimeType)) {
      // Try again, case-insensitively
      mimeType = BUILT_IN.getContentType(filename.toLowerCase());
    }
    return mimeType;
  }

}

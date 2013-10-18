package org.ambraproject.rhino.util;

import com.google.common.collect.ImmutableMap;

import javax.activation.MimetypesFileTypeMap;
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
  private static final String BUILT_IN_DEFAULT = "application/octet-stream";

  /*
   * From a legacy list of hard-coded constants (in "pmc2obj-v3.xslt"), mostly based on PLOS use cases.
   * Omits any case that appeared in "pmc2obj-v3.xslt" but was covered by MimetypesFileTypeMap as of Java 6.
   * Some cases omitted from here may not be covered in Java 7 due to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7096063
   */
  private static final Map<String, String> HARD_CODED = ImmutableMap.<String, String>builder()

      .put("asf", "video/x-ms-asf")
      .put("asx", "video/x-ms-asf")
      .put("bmp", "image/bmp")
      .put("bz2", "application/x-bzip")
      .put("bzip", "application/x-bzip")
      .put("csv", "text/comma-separated-values")
      .put("divx", "video/x-divx")
      .put("doc", "application/msword")
      .put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
      .put("dvi", "application/x-dvi")
      .put("eps", "application/eps")
      .put("gz", "application/x-gzip")
      .put("gzip", "application/x-gzip")
      .put("icb", "application/x-molsoft-icb")
      .put("latex", "application/x-latex")
      .put("m4v", "video/x-m4v")
      .put("mp2", "audio/mpeg")
      .put("mp3", "audio/x-mpeg3")
      .put("mp4", "video/mp4")
      .put("mpg4", "video/mp4")
      .put("pdf", "application/pdf")
      .put("png", "image/png")
      .put("pnm", "image/x-portable-any")
      .put("ppt", "application/vnd.ms-powerpoint")
      .put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
      .put("ra", "audio/x-realaudio")
      .put("ram", "audio/x-pn-realaudio")
      .put("rar", "application/x-rar-compressed")
      .put("ras", "image/x-cmu-raster")
      .put("rm", "audio/x-pn-realaudio")
      .put("rmi", "audio/midi")
      .put("rtf", "text/rtf")
      .put("snd", "audio/basic")
      .put("swf", "application/x-shockwave-flash")
      .put("tar", "application/x-tar")
      .put("wma", "audio/x-ms-wma")
      .put("wmv", "video/x-ms-wmv")
      .put("xls", "application/vnd.ms-excel")
      .put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
      .put("xml", "text/xml")
      .put("xpm", "image/x-xpix")
      .put("zip", "application/zip")

      .build();

  /*
   * A list of cases that should be covered by MimetypesFileTypeMap but for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7096063
   * Check for these before allowing MimetypesFileTypeMap to default to "application/octet-stream"
   */
  private static final Map<String, String> WORKAROUNDS = ImmutableMap.<String, String>builder()

      .put("aif", "audio/x-aiff")
      .put("aiff", "audio/x-aiff")
      .put("au", "audio/basic")
      .put("avi", "video/x-msvideo")
      .put("gif", "image/gif")
      .put("htm", "text/html")
      .put("html", "text/html")
      .put("ief", "image/ief")
      .put("jpg", "image/jpeg")
      .put("jpeg", "image/jpeg")
      .put("jpe", "image/jpeg")
      .put("mid", "audio/midi")
      .put("midi", "audio/midi")
      .put("mov", "video/quicktime")
      .put("qt", "video/quicktime")
      .put("mpg", "video/mpeg")
      .put("mpeg", "video/mpeg")
      .put("ps", "application/postscript")
      .put("tif", "image/tiff")
      .put("tiff", "image/tiff")
      .put("txt", "text/plain")
      .put("wav", "audio/x-wav")

      .build();


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
    String extension = getFileExtension(filename).toLowerCase();
    String mimeType = HARD_CODED.get(extension);
    if (mimeType != null) {
      return mimeType;
    }
    mimeType = BUILT_IN.getContentType(filename);
    if (BUILT_IN_DEFAULT.equals(mimeType)) {
      // Try again, case-insensitively
      mimeType = BUILT_IN.getContentType(filename.toLowerCase());
      if (BUILT_IN_DEFAULT.equals(mimeType)) {
        // Try again from the hard-coded list of cases that work around a possible bug in BUILT_IN
        String mimeTypeWorkaround = WORKAROUNDS.get(extension);
        if (mimeTypeWorkaround != null) {
          return mimeTypeWorkaround;
        }
      }
    }
    return mimeType;
  }

}

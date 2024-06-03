package org.raisercostin.jcrawl;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.vavr.control.Try;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

public class SlugEscape {
  private static final String MAX_FS_FILENAME_REACHED_SUFFIX = "--and-more";
  private static final int MAX_FS_FILENAME_LENGTH = 200;
  private static final int SPLIT_AT = 70;
  private static final Pattern nonUrlChars = Pattern.compile("[()\\[\\]{}_'\"`%^+_*!×&ƒ\\:? -]");//.r.unanchored
  private static final Pattern nonUrlPathChars = Pattern.compile("[\\/\\.]");

  public static String toSlug(String text) {
    //decode if url
    String result = Try.of(() -> java.net.URLDecoder.decode(text, "UTF-8")).getOrElse("");
    //convert to latin
    result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    result = result.toLowerCase();
    String code = Hashing.sha256().newHasher().putString(result, Charsets.UTF_8).hash().toString();
    String shortCode = "-#" + code.substring(0, 9) + "-";
    code = "--sha256-" + code;
    //int extensionPos = result.lastIndexOf(".");
    String extension = "";//StringUtils.substring(result, extensionPos==-1?-1:extensionPos+1);
    //result = StringUtils.substring(result, 0, extensionPos==-1?);
    val protocolEnds = result.indexOf("//");
    val firstSlash = result.indexOf("/",
      protocolEnds + 2);
    if (firstSlash == -1) {
      result = result.replaceAll("[.]", "--");
    } else if ((protocolEnds != -1) && (firstSlash != -1)) {
      result = result.substring(0, firstSlash).replaceAll("[.]", "--") + result.substring(firstSlash);
    }
    result = nonUrlChars.matcher(result).replaceAll("-");
    result = nonUrlPathChars.matcher(result).replaceAll("--");

    /*
     * //remove dot(.) except last
     * result = result.replaceFirst("\\.([^./]{1,5}$)", "--punct--final--$1")
     * result = result.replaceAllLiterally(".", "-")
     * result = result.replaceFirst("--punct--final--", ".")
     * //convert ()
     * //convert underlines
     * //convert apostrofe
     * result = result.replaceAll("[()\\[\\]{}]", "-")
     * result = result.replaceAllLiterally("_", "-")
     * result = result.replaceAll("['\"`]", "-")
     * //convert final non words
     * result = result.replaceAll("""[%^+_^*!×&ƒ\\:?]+""", "-")
     * //convert spaces
     * result = result.replaceAllLiterally(" ", "-")
     * //convert final non words
     * result = result.replaceAll("[-]+$", "")
     * //convert final non words
     * result = result.replaceAll("""[\/]+""", "--")
     * //val exceptLast = """\.(?=[^.]*\.)"""
     * //result = result.replaceAll(exceptLast,"-")
     * //println(s"toSlug($text)=[$result]")
     */
    return StringUtils.substring(result, 0, SPLIT_AT)
        + shortCode +
        StringUtils.abbreviate(StringUtils.substring(result, SPLIT_AT),
          MAX_FS_FILENAME_REACHED_SUFFIX,
          MAX_FS_FILENAME_LENGTH - shortCode.length() - code.length() - extension.length() - SPLIT_AT)
        + code
        + extension;
  }
}
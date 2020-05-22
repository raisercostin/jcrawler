package org.raisercostin.jcrawl;

import java.text.Normalizer;
import java.util.regex.Pattern;

import io.vavr.control.Try;
import lombok.val;

public class SlugEscape {
  private static final Pattern nonUrlChars = Pattern.compile("[()\\[\\]{}_'\"`%^+_*!×&ƒ\\:? -]");//.r.unanchored
  private static final Pattern nonUrlPathChars = Pattern.compile("[\\/]");

  public static String toSlug(String text) {
    //decode if url
    String result = Try.of(() -> java.net.URLDecoder.decode(text, "UTF-8")).getOrElse("");
    //convert to latin
    result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    result = result.toLowerCase();

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
    return result;
  }
}
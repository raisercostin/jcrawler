package org.raisercostin.jcrawler;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jedio.regex.RichRegex;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.Metadata;
import org.springframework.http.MediaType;

/**
 * Responsibility to create filenames from urls.
 * Filenames have some distinct limitations so is important to have a solution
 * that is
 * - deterministic
 * - low collision (two different urls not to map to same slug)
 * - human readability - important details to be present for human reading
 * - respect filename limits on filesystems in windows/linux/osx: for example
 * utf8?, max filenmae length, etc.
 * - [future] respect reversed hostname and dir/path structure since this will
 * improve human readability and lower the collisions
 * 
 * Current implementation is by truncating and adding some low collision
 * hash(sha256) of entire url.
 * 
 * For example:
 * ```
 * https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8
 * 
 * converted to slug
 * 
 * https-----posf--ro--comparator--api--index--php-request=comparator-ele-#8e0719e7b-ctric-tip-oferta=2-data-start-aplic--and-more--sha256-8e0719e7b97f46b0855f9e00932ea6495f227cdb7c5e1cc26ab942e271a56bcb
 * ```
 * 
 */
@AllArgsConstructor
@ToString
public class Slug {
  private static final String MAX_FS_FILENAME_REACHED_SUFFIX = "--and-more";
  private static final int MAX_FS_FILENAME_LENGTH = 200;
  private static final int SPLIT_AT = 70;
  private static final Pattern nonUrlChars = Pattern.compile("[()\\[\\]{}_'\"`%^+_*!×&ƒ\\:? -]");// .r.unanchored
  private static final Pattern nonUrlPathChars = Pattern.compile("[\\/\\.]");

  private Slug() {
  }

  public String code;
  public String slug;

  /**
   * Multiple types of encodings can exist: flat, dirs, etc. Return the default
   * one and the others to allow migrations.
   */
  public static RichIterable<Slug> slugs(String url) {
    return RichIterable.<Supplier<Slug>>of(
        () -> contentUid(url),
        () -> contentPathInitial(url))
        .map(x -> x.get());
  }

  static public Slug contentUid(String text) {
    String result = urlSanitized(text);
    String codeFull = urlHashed(result);
    return new Slug(codeFull, codeFull);
  }

  static public Slug contentPathInitial(String text) {
    String result = urlSanitized(text);
    String path = path(result, true) + "--" + urlHash(text).substring(0, 8) + ".tmp2";
    return new Slug("", path);
  }

  // TODO use metadata filetype and content-disposition
  public static Slug contentPathFinal(String externalForm, Metadata metadata) {
    Slug slug = contentPathInitial(externalForm);
    String path = slug.slug;
    path = StringUtils.removeEnd(path, ".tmp2");
    // .js .woff2 .htm .html
    Either<String, Tuple3<Matcher, String, String>> checkExtension = RichRegex.regexp2("^(.*/.*)\\.(.{2,5})$", path);
    String extension = "";
    if (checkExtension.isRight()) {
      path = checkExtension.get()._2;
      extension = "." + checkExtension.get()._3;
    }
    if (extension.isEmpty() && metadata.responseHeaders != null) {
      MediaType contentType = metadata.contentType();
      if (contentType != null) {
        extension = "." + contentType.getSubtype();
      }
    }
    if (metadata.statusCodeValue != 200) {
      path = path + ".E" + metadata.statusCodeValue;
    }
    slug.slug = path + extension;
    return slug;
  }
  //
  // static Option<String> getFileExtensionFromContentType(String contentType) {
  // try {
  // return Option.of(Files.probeContentType(Path.of("dummy." + contentType)));
  // } catch (IOException e) {
  // throw org.jedio.RichThrowable.nowrap(e);
  // }
  // }

  static public Slug contentPathInitialOld(String text) {
    String result = urlSanitized(text);
    String codeFull = urlHashed(result);
    String shortCode = "-#" + codeFull.substring(0, 9) + "-";
    String code = "--sha256-" + codeFull;
    result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    // int extensionPos = result.lastIndexOf(".");
    String extension = "";// StringUtils.substring(result, extensionPos==-1?-1:extensionPos+1);
    // result = StringUtils.substring(result, 0, extensionPos==-1?);
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
    return new Slug(codeFull, StringUtils.substring(result, 0, SPLIT_AT)
        + shortCode +
        StringUtils.abbreviate(StringUtils.substring(result, SPLIT_AT),
            MAX_FS_FILENAME_REACHED_SUFFIX,
            MAX_FS_FILENAME_LENGTH - shortCode.length() - code.length() - extension.length() - SPLIT_AT)
        + code
        + extension + ".tmp");
  }

  /**
   * A has computed for url after sanitization (removal of fragments, but keeping
   * query params).
   */
  public static String urlHash(String url) {
    return urlHashed(urlSanitized(url));
  }

  public static String urlHashed(String result) {
    return Hashing.sha256().newHasher().putString(result, Charsets.UTF_8).hash().toString();
  }

  /**
   * Remove fragments (#fragments). For now query params (?queryParams) are not
   * removed.
   */
  public static String urlSanitized(String url) {
    // decode if url
    String result = Try.of(() -> java.net.URLDecoder.decode(url, "UTF-8")).get();
    // convert to latin
    // result = result.toLowerCase();
    // remove fragments
    result = RichRegex.replaceAll(result, "([^#]+)", "$1");
    return result;
  }

  public static String path(String url) {
    return path(url, false);
  }

  /**
   * The url converted to a relative path. Use @ instead of ? as wget. Fragments
   * should get discarded.
   */
  public static String path(String url, boolean stripQueryParams) {
    var result = urlSanitized(url);
    if (stripQueryParams) {
      result = RichRegex.replaceAll(result, "\\?.*$", "");
    } else {
      result = RichRegex.replaceAll(result, "\\?", "@");
    }
    result = RichRegex.replaceAll(result, "^([^:]+://)", "");
    result = transformFileName(result);// RichRegex.replaceAll(result, ":", "%3A");
    if (result.endsWith("/")) {
      return result + "/index.html";
    }
    return result;
  }

  private static final Map<Character, String> charReplacements = new HashMap<>();

  static {
    charReplacements.put(':', "\uF03A"); // Unicode for colon or a custom replacement
    // charReplacements.put('\\', "\uF07C"); // Unicode for backslash
    // charReplacements.put('/', "\uF0F7"); // Unicode for forward slash
    charReplacements.put('*', "\uF0A1"); // Unicode for asterisk
    charReplacements.put('?', "\uF03F"); // Unicode for question mark
    charReplacements.put('"', "\uF022"); // Unicode for double quote
    charReplacements.put('<', "\uF03C"); // Unicode for less than
    charReplacements.put('>', "\uF03E"); // Unicode for greater than
    charReplacements.put('|', "\uF07C"); // Unicode for pipe
  }

  // Function to transform a string to a Windows-safe file name
  private static String transformFileName(String input) {
    StringBuilder result = new StringBuilder();

    for (char c : input.toCharArray()) {
      if (charReplacements.containsKey(c)) {
        result.append(charReplacements.get(c));
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

}
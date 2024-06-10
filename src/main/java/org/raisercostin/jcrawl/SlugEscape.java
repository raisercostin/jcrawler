package org.raisercostin.jcrawl;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.vavr.control.Try;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 Responsibility to create filenames from urls.
 Filenames have some distinct limitations so is important to have a solution that is
 - deterministic
 - low collision (two different urls not to map to same slug)
 - human readability - important details to be present for human reading
 - respect filename limits on filesystems in windows/linux/osx: for example utf8?, max filenmae length, etc.
 - [future] respect reversed hostname and dir/path structure since this will improve human readability and lower the collisions

Current implementation is by truncating and adding some low collision hash(sha256) of entire url.

For example:
```
https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8

converted to slug

https-----posf--ro--comparator--api--index--php-request=comparator-ele-#8e0719e7b-ctric-tip-oferta=2-data-start-aplic--and-more--sha256-8e0719e7b97f46b0855f9e00932ea6495f227cdb7c5e1cc26ab942e271a56bcb
```

 */
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
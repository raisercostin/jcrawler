package org.raisercostin.jcrawl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawl.JCrawler.Crawler;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.nodes.Nodes;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class AnreSpotTest {
  @ToString
  public static class Judet {
    public final int idJudet;
    public final String nume;
    public final int idZona;
    public final String numeZona;

    @JsonCreator
    public Judet(
        @JsonProperty("id_judet") int idJudet,
        @JsonProperty("nume") String nume,
        @JsonProperty("id_zona") int idZona,
        @JsonProperty("nume_zona") String numeZona)
    {
      this.idJudet = idJudet;
      this.nume = nume;
      this.idZona = idZona;
      this.numeZona = numeZona;
    }
  }

  @ToString
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Oferta {
    public String ofertaPdf;
    public String idOferta;
    public String denumireOferta;
    public String legaturaOferta;
    public Furnizor furnizor;
    public String numeFurnizor;
    public String tipOferta;
    public String tipProdus;
    public String procentEnergieSurseRegenerabile;
    public String valoareFacturaFurnizorFc;
    public String pretFurnizare;
    public String pretFinal;
    public String rezultatComparatieValoareFactura;
    public String rezultatEconomieCost;
    public String tipClient;
    public String tipPretOferta;
    public String valabilitateOfertaStart;
    public String valabilitateOfertaStop;
    public String perioadaDeAplicareStart;
    public String perioadaDeAplicareStop;
    public String intervalDiferentiat;
    public String unitateMasura;
    public String pretEnergie;
    public String valoareComponentaFixa;
    public String tarifTransportTl;
    public String tarifServiciuSistem;
    public String tarifServiciuDistributie;
    public String taxaCogenerareInaltaEficienta;
    public String contravaloareCertificateVerzi;
    public String acciza;
    public String tva;
    public String perioadaContract;
    public String termenPlata;
    public String modTransmitereFactura;
    public String frecventaEmitereFactura;
    public String frecventaCitireContor;
    public List<String> modelContract;
    public String alteCosturi;
    public int anAchizitieEnergie;
    public StructuraAchizitieEnergetica structuraAchizitieEnergetica;
    public String reduceri;
    public String perioadaReduceriMin;
    public String perioadaReduceriMax;
    public String serviciiAditionaleIncluse;
    public String ultimaActualizare;
    public String alteConditii;
    public List<Document> documente;
    public String codOferta;
    public int posfReady;
  }

  @ToString
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Furnizor {
    public int idFurnizor;
    public String numeFurnizor;
    public String numePersoanaContact;
    public String telefonPersoanaContact;
    public String emailPersoanaContact;
    public String website;
    public String tel;
    public String adresa;
    public String emailContract;
    public String faxContract;
    public String linkContract;
    public String alteModalitatiContract;
    public int ultimaInstanta;
    public String pretClientCasnic;
    public String pretClientCasnicMt;
    public String perioadaAplicareMinPretClientCasnic;
    public String perioadaAplicareMaxPretClientCasnic;
    public String pretClientNoncasnicNuSu;
    public String perioadaAplicareMinPretClientNoncasnicNuSu;
    public String perioadaAplicareMaxPretClientNoncasnicNuSu;
    public String pretClientNoncasnicSu;
    public String perioadaAplicareMinPretClientNoncasnicSu;
    public String perioadaAplicareMaxPretClientNoncasnicSu;
    public int ofertaClientNouCasnic;
    public int ofertaClientNouNoncasnic;
    public int ofertaClientFurnizorExistentCasnic;
    public int ofertaClientFurnizorExistentNoncasnic;
    public int ofertaClientFurnizorNouCasnic;
    public int ofertaClientFurnizorNouNoncasnic;
    public String ccv;
    public String perioadaAplicareMin;
    public String perioadaAplicareMax;
    public String contractCadru;
    public String zonaDeActivitate;
    public String idAgent;
    public String dataAdaugarii;
    public String ultimaActualizare;
    public boolean sters;
    public String codLicenta;
  }

  @ToString
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Document {
    public int idDocument;
    public int idFurnizor;
    public String nume;
    public String fisier;
    public String dataAdaugarii;
    public String tipDocument;
  }

  @ToString
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class StructuraAchizitieEnergetica {
    public String gaz;
    public String hidro;
    public String nucleara;
    public String vant;
    public String solar;
    public String altele;
  }

  public static <T> Function<T, T> tap(Consumer<? super T> consumer) {
    return v -> {
      consumer.accept(v);
      return v;
    };
  }

  public static class AnreDatabase {
    public Zona[] zone;
    public List<Furnizor> furnizori;
    public List<Oferta> oferte;
  }

  public static class Zona {
    public String cod;
    public String nume;
    public Judet[] judete;
  }

  @Test
  void test() {
    String judete = "https://posf.ro/comparator/api/index.php?request=get-judete";
    PathLocation workdir = Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan6-anre");
    Crawler crawler = Crawler
      .crawler()
      .withCache(workdir);
    crawler
      .start(judete)
      .crawl()
      .forEach(x -> System.out.println("aici" + x));

    List<Judet> content = Nodes.json.toList(crawler.cachedFile(judete).readContent(), Judet.class);
    Seq<Tuple2<String, Iterator<Judet>>> allZones = Iterator.ofAll(content)
      .groupBy(x -> x.idZona + "-" + x.numeZona)
      .toList()
      .sortBy(x -> x._1)
      .map(tap(System.out::println));

    String dataStart = "02-06-2024";
    String tipClient = "casnic";
    String consumLunar = "100";
    String url1 = """
        https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=0&data_start_aplicare=%s&tip_client=%s&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=%s&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor="""
      .formatted(dataStart, tipClient, consumLunar);

    Seq<Tuple2<Integer, String>> offers = allZones
      .map(x -> x._2.head().idZona)
      .map(zone -> {
        String offer = crawler
          .withGenerator(url1 + "&id_zona={" + zone + "}")
          .crawl()
          .map(tap(x -> System.out.println("oferte " + x)))
          .head();
        return Tuple.of(zone, offer);
      })
      .toList();

    Map<Integer, List<Oferta>> allOffers = offers.map(offer -> {
      List<Oferta> content1 = Nodes.json.toList(crawler.cachedFile(offer._2).readContent(), Oferta.class);
      Iterator.ofAll(content1).forEach(x -> System.out.println(x));
      return Tuple.of(offer._1, content1);
    }).toJavaMap(x -> x);

    AnreDatabase database = new AnreDatabase();
    database.oferte = Iterator
      .ofAll(allOffers.entrySet())
      .flatMap(x -> Iterator.ofAll(x.getValue()))
      .groupBy(x -> x.idOferta)
      //TODO check all offers are identical
      .map(x -> x._2.head())
      .toJavaList();
    database.furnizori = Iterator.ofAll(database.oferte)
      .map(x -> x.furnizor)
      .groupBy(x -> x.numeFurnizor)
      .map(x -> x._2.head())
      .toJavaList();

    String furnizoriContent = Nodes.csv.toString(database.furnizori);
    System.out.println(furnizoriContent);
    workdir.child("furnizori-" + consumLunar + ".csv").write(furnizoriContent);

    String oferteContent = Nodes.csv.toString(database.oferte);
    System.out.println(oferteContent);
    workdir.child("oferte-" + consumLunar + ".csv").write(oferteContent);

    String allContent = new Yaml().dump(database);
    //allContent = deduplicate2(allContent);
    //String allContent = Nodes.json.toString(allOffers);
    workdir.child("all.yaml").write(allContent);
  }

  private String deduplicate2(String allContent) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);

    Yaml yaml = new Yaml(new Constructor(new LoaderOptions()), new CustomRepresenter(options), options);

    Object data = yaml.load(allContent);

    Map<Object, Object> referenceMap = new IdentityHashMap<>();
    Object deduplicatedData = deduplicate(data, referenceMap, new HashMap<>());

    return yaml.dump(deduplicatedData);
  }

  private static Object deduplicate(Object data, Map<Object, Object> referenceMap, Map<Object, Object> equalityMap) {
    if (data instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) data;
      for (Map.Entry<Object, Object> entry : equalityMap.entrySet()) {
        if (entry.getKey() instanceof Map && mapsAreEqual(map, (Map<?, ?>) entry.getKey())) {
          return entry.getValue();
        }
      }
      Map<Object, Object> deduplicatedMap = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object key = deduplicate(entry.getKey(), referenceMap, equalityMap);
        Object value = deduplicate(entry.getValue(), referenceMap, equalityMap);
        deduplicatedMap.put(key, value);
      }
      equalityMap.put(map, deduplicatedMap);
      referenceMap.put(map, deduplicatedMap);
      return deduplicatedMap;
    } else if (data instanceof List) {
      List<Object> list = (List<Object>) data;
      for (Map.Entry<Object, Object> entry : equalityMap.entrySet()) {
        if (entry.getKey() instanceof List && listsAreEqual(list, (List<?>) entry.getKey())) {
          return entry.getValue();
        }
      }
      List<Object> deduplicatedList = new ArrayList<>();
      for (Object item : list) {
        deduplicatedList.add(deduplicate(item, referenceMap, equalityMap));
      }
      equalityMap.put(list, deduplicatedList);
      referenceMap.put(list, deduplicatedList);
      return deduplicatedList;
    } else {
      for (Object key : equalityMap.keySet()) {
        if (Objects.equals(data, key)) {
          return equalityMap.get(key);
        }
      }
      equalityMap.put(data, data);
      referenceMap.put(data, data);
      return data;
    }
  }

  /**
   * Computes the distance score between two maps based on the number of identical key-value pairs
   * and the differences in the number of keys.
   *
   * @param map1 the first map
   * @param map2 the second map
   * @return the distance score
   */
  public static int computeDistance(Map<?, ?> map1, Map<?, ?> map2) {
    int matchingPairs = 0;

    for (Map.Entry<?, ?> entry : map1.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();

      if (map2.containsKey(key) && Objects.equals(value, map2.get(key))) {
        matchingPairs++;
      }
    }

    int totalKeys = map1.size() + map2.size();
    int nonMatchingPairs = totalKeys - 2 * matchingPairs;

    return nonMatchingPairs;
  }

  private static boolean mapsAreEqual(Map<?, ?> map1, Map<?, ?> map2) {
    if (map1.size() != map2.size()) {
      return false;
    }
    for (Map.Entry<?, ?> entry : map1.entrySet()) {
      if (!map2.containsKey(entry.getKey()) || !Objects.equals(entry.getValue(), map2.get(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private static boolean listsAreEqual(List<?> list1, List<?> list2) {
    if (list1.size() != list2.size()) {
      return false;
    }
    for (int i = 0; i < list1.size(); i++) {
      if (!Objects.equals(list1.get(i), list2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static class CustomRepresenter extends Representer {
    public CustomRepresenter(DumperOptions options) {
      super(options);
      this.representers.put(AnchorNode.class, new RepresentAnchorNode());
    }

    private class RepresentAnchorNode implements Represent {
      @Override
      public Node representData(Object data) {
        AnchorNode node = (AnchorNode) data;
        Node valueNode = represent(node.getValue());
        valueNode.setAnchor(node.getAnchor());
        return valueNode;
      }
    }
  }

  private static class AnchorNode {
    private final Object value;
    private final String anchor;

    public AnchorNode(Object value, String anchor) {
      this.value = value;
      this.anchor = anchor;
    }

    public Object getValue() {
      return value;
    }

    public String getAnchor() {
      return anchor;
    }
  }

  @Test
  void testdedup() {
    String result = deduplicate2(
      """
          1:
          -
            acciza: '0.00682'
            alteConditii: Plata in avans
            alteCosturi: Tarife reglementate in functie de zona de distributie
            furnizor: {adresa: 'Str. Aviator Popișteanu, nr. 54A, clădirea 1, etaj 4, sector
                1, clădirea Expo Business Park, Bucuresti', alteModalitatiContract: '', ccv: '0',
              ultimaActualizare: '2022-08-05 07:29:23+00', ultimaInstanta: 0, website: 'https://ro.met.com/ro/',
              zonaDeActivitate: ''}
            furnizor2: {adresa: 'Str. Aviator Popișteanu, nr. 54A, clădirea 1, etaj 4, sector
                1, clădirea Expo Business Park, Bucuresti', alteModalitatiContract: '', ccv: '0',
              ultimaActualizare: '2022-08-05 07:29:23+00', ultimaInstanta: 0, website: 'https://ro.met.com/ro/',
              zonaDeActivitate: ''}
          2:
          -
            acciza: '0.00682'
            alteConditii: Plata in avans
            alteCosturi: Tarife reglementate in functie de zona de distributie
            furnizor: {adresa: 'Str. Aviator Popișteanu, nr. 54A, clădirea 1, etaj 4, sector
                1, clădirea Expo Business Park, Bucuresti', alteModalitatiContract: '', ccv: '0',
              ultimaActualizare: '2022-08-05 07:29:23+00', ultimaInstanta: 0, website: 'https://ro.met.com/ro/',
              zonaDeActivitate: ''}
          """);

    assertThat(result).isEqualTo("""
        1:
        - &ref_0
          acciza: '0.00682'
          alteConditii: Plata in avans
          alteCosturi: Tarife reglementate in functie de zona de distributie
          furnizor: &id001 {adresa: 'Str. Aviator Popișteanu, nr. 54A, clădirea 1, etaj 4,
              sector 1, clădirea Expo Business Park, Bucuresti', alteModalitatiContract: '',
            ccv: '0', ultimaActualizare: '2022-08-05 07:29:23+00', ultimaInstanta: 0, website: 'https://ro.met.com/ro/',
            zonaDeActivitate: ''}
          furnizor2: *id001
        2:
        - *ref_0
          furnizor2: null
        """);
  }
}

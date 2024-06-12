package org.raisercostin.jcrawler;

import io.vavr.collection.Array;
import io.vavr.collection.Seq;
import org.raisercostin.jedio.Location;
import org.raisercostin.jedio.Locations;
import picocli.CommandLine;

public class RichPicocli {
  public static class LocationConverter implements CommandLine.ITypeConverter<Location> {
    @Override
    public Location convert(String value) throws Exception {
      return Locations.path(value);
    }
  }

  public static class VavrConverter implements CommandLine.ITypeConverter<Seq> {
    @Override
    public Seq convert(String value) throws Exception {
      return Array.of(value.split(","));
    }
  }
}

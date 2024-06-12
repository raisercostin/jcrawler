package org.raisercostin.jcrawler;

import io.vavr.collection.Array;
import io.vavr.collection.Seq;
import lombok.AllArgsConstructor;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Location;
import org.raisercostin.jedio.Locations;
import picocli.CommandLine;

public class RichPicocli {
  /**Default value for projectDir uses toString so this wrapper is for this.*/
  @AllArgsConstructor
  public static class PicocliDir {
    public DirLocation dir;

    @Override
    public String toString() {
      return dir.absoluteAndNormalized();
    }
  }

  public static class LocationConverter implements CommandLine.ITypeConverter<PicocliDir> {
    @Override
    public PicocliDir convert(String value) throws Exception {
      return new PicocliDir(Locations.path(value));
    }
  }

  public static class VavrConverter implements CommandLine.ITypeConverter<Seq> {
    @Override
    public Seq convert(String value) throws Exception {
      return Array.of(value.split(","));
    }
  }
}

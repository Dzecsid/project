import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Statistics {
    private final List<Region> Regions;
    private final List<Rank> Ranks;
    private final List<Scores> Scores;

    public Statistics(Path path) {
        var regions = new ArrayList<Region>();
        var ranks = new ArrayList<Rank>();
        var scores = new ArrayList<Scores>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.readLine();
            while (br.ready()) {
                var row = splitRow(br.readLine());
                regions.add(new Region(row[0], row[1]));
                ranks.add(new Rank(row[0], row[2]));
                scores.add(new Scores(row[0], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11], row[12]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Regions = regions;
        Ranks = ranks;
        Scores = scores;
    }

    private String[] splitRow(String row) {
        var result = row.split(",");
        return Arrays.copyOfRange(result, 0, 13);
    }

    public List<Region> getRegions() { return Regions; }

    public List<Rank> getRanks() {
        return Ranks;
    }

    public List<Scores> getScores() {
        return Scores;
    }
}

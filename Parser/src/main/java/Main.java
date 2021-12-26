import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import javax.swing.*;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main
{
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Распарсить CSV? | + or -\n");
        var line = in.next();
        Statistics statistics;
        if (line.equals("+")) {
            System.out.println("WAIT...");
            statistics = new Statistics(Paths.get("Показатель счастья по странам 2016.csv")
                    .toAbsolutePath());
            SaveToDatabase(statistics);
        }
        DrawDiagrams();
        System.out.println("Task 2:\n");
        Task2("generosity", "Middle East and Northern Africa");
        Task2("generosity", "Central and Eastern Europe");
        System.out.println("\nTask 3:\n");
        Task3("happinessScore", "Southeastern Asia");
        Task3("happinessScore", "Sub-Saharan Africa");
    }

    public static void Task2(String column, String region) {
        System.out.println(String.format("Min %s for \"%s\": ", column, region)
                + GetCountryMinScore(column, region));
    }

    public static void Task3(String column, String region) {
        String query1 = String.format("SELECT region.country, region.region, ABS(scores.%s - a) as a\n" +
                "                FROM region\n" +
                "                LEFT JOIN scores ON scores.country = region.country\n" +
                "                LEFT JOIN (SELECT AVG(scores.%s) as a, region.region as b\n" +
                "                    FROM scores\n" +
                "                    LEFT JOIN region ON region.country = scores.country\n" +
                "                    WHERE b = '%s')\n" +
                "                WHERE region = '%s'\n" +
                "                ORDER BY a\n" +
                "                LIMIT 1", column, column, region, region);

        try {
            var set1 = MakeExecuteQuery(query1);
            assert set1 != null;
            set1.next();
            System.out.println("Среднее значение: " + set1.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String GetCountryMinScore(String column, String region) {
        String query = String.format("SELECT scores.country, scores.%s, region.region\n" +
                                     "FROM scores\n" +
                                     "LEFT JOIN region ON region.country = scores.country\n" +
                                     "WHERE region = '%s'\n" +
                                     "ORDER BY %s\n" +
                                     "LIMIT 1" , column , region , column);
        String minGenerosityValue = null;
        try {
            var set = MakeExecuteQuery(query);
            assert set != null;
            set.next();
            minGenerosityValue = set.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return minGenerosityValue;
    }

    public static void DrawDiagrams() {
        JFreeChart chart = ChartFactory.createBarChart(
                "График по показателю щедрости объеденый по странам",
                null,
                "Щедрость",
                GetDataSet(MakeExecuteQuery("SELECT country, generosity FROM scores")));

        JFrame frame = new JFrame("Generosity");
        frame.getContentPane().add(new ChartPanel(chart));
        frame.setSize(600,400);
        frame.setVisible(true);
    }

    public static CategoryDataset GetDataSet(CachedRowSet set) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try {
            while (set.next()) {
                dataset.addValue(set.getDouble(2), set.getString(1), "Регионы");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataset;
    }

    public static CachedRowSet MakeExecuteQuery(String query)
    {
        Connection connection = null;
        Statement statement = null;
        try {
            RowSetFactory factory = RowSetProvider.newFactory();
            CachedRowSet set = factory.createCachedRowSet();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:identifier.sqlite");
            statement = connection.createStatement();

            var resSet = statement.executeQuery(query);
            set.populate(resSet);

            return set;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert statement != null;
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void SaveToDatabase(Statistics statistics) {
        Connection connection = null;
        Statement regionStatement = null;
        Statement rankStatement = null;
        Statement scoresStatement = null;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:identifier.sqlite");
            regionStatement = connection.createStatement();
            rankStatement = connection.createStatement();
            scoresStatement = connection.createStatement();
            for (var i:statistics.getRegions()) {
                regionStatement.executeUpdate(String.format("INSERT INTO region values ('%s', '%s')",
                        i.getCountry(),
                        i.getRegion()));
            }

            for (var i:statistics.getRanks()) {
                rankStatement.executeUpdate(String.format("INSERT INTO rank values ('%s', '%s')",
                        i.getCountry(),
                        i.getHappinessRank()));
            }

            for (var i:statistics.getScores()) {
                scoresStatement.executeUpdate(String.format("INSERT INTO scores values ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                        i.getCountry(),
                        i.getHappinessScore(),
                        i.getLowerConfidence(),
                        i.getEconomy(),
                        i.getFamily(),
                        i.getHealth(),
                        i.getFreedom(),
                        i.getTrust(),
                        i.getGenerosity(),
                        i.getDystopiaResidual(),
                        i.getUpperConfidence()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert regionStatement != null;
                regionStatement.close();
                assert rankStatement != null;
                rankStatement.close();
                assert scoresStatement != null;
                scoresStatement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

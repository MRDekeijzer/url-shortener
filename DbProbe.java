import org.postgresql.ds.PGSimpleDataSource;

public class DbProbe {
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "jdbc:postgresql://localhost:5432/minurl";
        System.out.println("Connecting to: " + url);
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser("minurl");
        ds.setPassword("minurl");
        try (var conn = ds.getConnection()) {
            System.out.println("Connected to " + conn.getCatalog());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

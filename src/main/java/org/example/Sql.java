package org.example;

import org.example.model.Article;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {

    private final Connection conn;

    private StringBuilder sb = new StringBuilder();
    private List<Object> params = new ArrayList<>(); // 1 2 3 1 2 3

    public Sql(Connection conn) {
        this.conn = conn;
    }


    //todo param 저장해서 나중에 바인딩
    public Sql append(String sqlBit, Object... param) {
        sb.append(sqlBit).append(" ");
        this.params.addAll(Arrays.asList(param));
        return this;
    }

    public Sql appendIn(String sqlBit, Object... params) {

        int paramSize = params.length;
        List<String> placeholder = Collections.nCopies(paramSize, "?");
        String result = String.join(", ", placeholder); // ?, ?, ?

        sb.append(sqlBit.replace("?", result)).append(" ");

        this.params.addAll(Arrays.asList(params));
        return this;
    }

    public long insert() {
        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(pst, params);
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new SQLException("Don't find Key");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }


    private void bindParameters(PreparedStatement pst, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pst.setObject(i + 1, params.get(i));
        }
    }


    public int update() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {

            bindParameters(pst, params);
            return pst.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }

    public int delete() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            bindParameters(pst, params);
            return pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        String sql = sb.toString();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                Map<String, Object> row = new HashMap<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object content = rs.getObject(i);

                    if (content instanceof Timestamp) {
                        content = ((Timestamp) content).toLocalDateTime();
                    }

                    row.put(columnName, content);
                }
                rows.add(row);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }


        return rows;
    }

    public <T> List<T> selectRows(Class<T> model) {

        String sql = sb.toString();
        List<T> rows = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            // T 에는 엔티티 객체
            while (rs.next()) {
                T article = mapToEntity(model, rs);
                rows.add(article);

            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
        return rows;

    }

    private <T> T mapToEntity(Class<T> model, ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        T article = null;
        try {
            if (model == Article.class) {
                Long id = rs.getLong(1);
                LocalDateTime createdDate = rs.getTimestamp(2).toLocalDateTime();
                LocalDateTime modifiedDate = rs.getTimestamp(3).toLocalDateTime();
                String tile = rs.getString(4);
                String body = rs.getString(5);
                boolean blind = rs.getBoolean(6);
                article = (T) Article.of(id, tile, body, createdDate, modifiedDate, blind);
            } else {
                // 추후 스펙에 맞게 변경
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating object from ResultSet: " + e.getMessage(), e);
        }
        return article;
    }

    public Map<String, Object> selectRow() {
        String sql = sb.toString();
        Map<String, Object> row = new HashMap<>();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object content = rs.getObject(i);

                    if (content instanceof Timestamp) {
                        content = ((Timestamp) content).toLocalDateTime();
                    }
                    row.put(columnName, content);
                }
            } else {
                throw new NoSuchElementException("Not Found Data");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
        return row;
    }

    public <T> T selectRow(Class<T> model) {
        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                return mapToEntity(model, rs);
            } else {
                throw new NoSuchElementException("Not Found Data");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }


    public LocalDateTime selectDatetime() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            } else {
                throw new NoSuchElementException("Not Found Data");
            }


        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }


    }

    public String selectString() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                return rs.getString(1);
            } else {
                throw new NoSuchElementException("Not Found Data");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }

    }

    public Boolean selectBoolean() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                return rs.getBoolean(1);
            } else {
                throw new NoSuchElementException("Not Found Data");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }

    public Long selectLong() {

        String sql = sb.toString();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {


            if (!params.isEmpty()) {
                bindParameters(pst, params);
            }

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new NoSuchElementException("Not Found Data");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
    }


    public List<Long> selectLongs() {
        String sql = sb.toString();
        List<Long> foundIds = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            bindParameters(pst, params);
            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    foundIds.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL : " + sql + " Error : " + e.getMessage(), e);
        }
        return foundIds;
    }
}

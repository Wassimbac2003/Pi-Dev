package Services;

import java.sql.SQLException;
import java.util.List;

public interface ICrud<T> {
    public void insert(T obj) throws SQLException;

    public void update(T obj) throws SQLException;

    public void delete(T obj) throws SQLException;

    public List<T> findAll() throws SQLException;

}


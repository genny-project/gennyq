package life.genny.bootq.bootxport.utils;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public interface StorageUtils {


  static SessionFactory sessionFactory =
      HibernateUtil.getSessionFactory();
  static Session session = sessionFactory.openSession();


  @SuppressWarnings("unchecked")
  public static <T> List<T> fetchAll(String query) {

    return session.createQuery(query).getResultList();
  }

}

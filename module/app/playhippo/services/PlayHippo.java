package playhippo.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayHippo {

    public static HippoRepository repo;
    public static Session session;

    @Inject
    public PlayHippo(play.inject.ApplicationLifecycle appLifecycle) {
        // TODO handle reconnect / timeout?
        Config config = ConfigFactory.load();
        try {
            repo = HippoRepositoryFactory.getHippoRepository(config.getString("hippo.rmi.uri"));
            session = repo.login(config.getString("hippo.rmi.user"), config.getString("hippo.rmi.password").toCharArray());

            Logger.info("Hippo repository connection established");
        } catch (RepositoryException e) {
            Logger.error("Exception occurred, no repository session available.", e);
        }

        appLifecycle.addStopHook(() -> {
            session.logout();
            repo.close();
            return CompletableFuture.completedFuture(null);
        });
    }


    private static ObjectConverter getObjectConverter() throws ClassNotFoundException {
        return ObjectConverterUtils.createObjectConverter(getAnnotatedClasses(), true);
    }


    private static Collection<Class<? extends HippoBean>> getAnnotatedClasses() throws ClassNotFoundException {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        // TODO load class names from configuration
        Class<?> bean = Class.forName("model.HippoGoGreenNewsDocument");
        annotatedClasses.add((Class<? extends HippoBean>) bean);
        return annotatedClasses;
    }


    public static HstQuery createQuery(String folderPath) throws QueryException, ObjectBeanManagerException, ClassNotFoundException {
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(PlayHippo.session, objectConverter);
        HippoFolder folder = (HippoFolder) obm.getObject(folderPath);

        HstQueryManager queryManager = new HstQueryManagerImpl(PlayHippo.session, objectConverter, null);
        return queryManager.createQuery(folder);
    }

    // TODO add more helper methods

}

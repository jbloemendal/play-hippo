package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayHippo {

    public static HippoRepository repo;
    public static Session session;

    @Inject
    public PlayHippo(play.inject.ApplicationLifecycle appLifecycle) {

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

}

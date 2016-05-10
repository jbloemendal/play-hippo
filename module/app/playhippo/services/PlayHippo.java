package playhippo.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class PlayHippo {

    public static HippoRepository repo;
    public static Session session;
    public static Collection<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();

    @Inject
    public PlayHippo(play.inject.ApplicationLifecycle appLifecycle) {
        Config config = ConfigFactory.load();
        try {
            annotatedClasses = getAnnotatedClasses();

            repo = HippoRepositoryFactory.getHippoRepository(config.getString("hippo.rmi.uri"));
            session = repo.login(config.getString("hippo.rmi.user"), config.getString("hippo.rmi.password").toCharArray());

            Logger.info("Hippo repository connection established");
        } catch (RepositoryException e) {
            Logger.error("Exception occurred, no repository session available.", e);
        } catch (ClassNotFoundException e) {
            Logger.error("Exception occurred, loading annotated classes.", e);
        }

        appLifecycle.addStopHook(() -> {
            session.logout();
            repo.close();
            return CompletableFuture.completedFuture(null);
        });
    }


    private static ObjectConverter getObjectConverter() throws ClassNotFoundException {
        return ObjectConverterUtils.createObjectConverter(annotatedClasses, true);
    }


    private static Collection<Class<? extends HippoBean>> getAnnotatedClasses() throws ClassNotFoundException {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Node.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("model")) {
            Class<?> bean = Class.forName(bd.getBeanClassName());
            annotatedClasses.add((Class<? extends HippoBean>) bean);
        }

        System.out.println(annotatedClasses.toString());
        return annotatedClasses;
    }


    public static HstQuery createQuery(String folderPath) throws QueryException, ObjectBeanManagerException, ClassNotFoundException {
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(PlayHippo.session, objectConverter);
        HippoFolder folder = (HippoFolder) obm.getObject(folderPath);

        HstQueryManager queryManager = new HstQueryManagerImpl(PlayHippo.session, objectConverter, null);
        return queryManager.createQuery(folder);
    }


    public static HippoFolderBean createFolder(String newFolderNodePath, String hippoStdFolderNodeType, String folderName)
            throws ClassNotFoundException, ObjectBeanManagerException {
        ObjectConverter objectConverter = getObjectConverter();

        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, objectConverter);

        HippoFolderBean newFolder = null;

        // create a document with type and name
        String absoluteCreatedDocumentPath = wpm.createAndReturn(newFolderNodePath, hippoStdFolderNodeType, folderName, true);

        // retrieves the document created just before
        newFolder = (HippoFolderBean) wpm.getObject(absoluteCreatedDocumentPath);

        wpm.save();

        return newFolder;
    }


    public static HippoBean newDocument(String folderNodePath, String documentType, String newDocumentNodeName)
            throws ClassNotFoundException, ObjectBeanManagerException {
        ObjectConverter objectConverter = getObjectConverter();

        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, objectConverter);

        // create a document with type and name
        String absoluteCreatedDocumentPath = wpm.createAndReturn(folderNodePath, documentType, newDocumentNodeName, false);
        // retrieves the document created just before
        return (HippoBean) wpm.getObject(absoluteCreatedDocumentPath);
    }


    public static Session getSession() {
        return session;
    }
}

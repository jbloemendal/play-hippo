package org.onehippo.playhippo.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
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
import org.springframework.core.type.filter.AnnotationTypeFilter;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayHippoTool {

    public static HippoRepository repo;
    public static Session session;
    public static Collection<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();

    @Inject
    public PlayHippoTool(play.inject.ApplicationLifecycle appLifecycle) {
        Config config = ConfigFactory.load();
        try {
            annotatedClasses = getAnnotatedClasses();

            String uri = config.getString("hippo.rmi.uri");
            if (StringUtils.isEmpty(uri)) {
                Logger.error("config parameter hippo.rmi.uri is missing.");
                return;
            }

            String user = config.getString("hippo.rmi.user");
            if (StringUtils.isEmpty(user)) {
                Logger.error("config parameter hippo.rmi.user is missing.");
                return;
            }

            char[] password = config.getString("hippo.rmi.password").toCharArray();
            if (password == null || StringUtils.isEmpty(password.toString())) {
                Logger.error("config parameter hippo.rmi.password is missing.");
                return;
            }

            repo = HippoRepositoryFactory.getHippoRepository(uri);
            session = repo.login(user, password);

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

        return annotatedClasses;
    }

    /**
     * Retrieve a hippo bean (document, folder ...)
     * @param path the path to the document
     * @return the hippo bean
     * @throws ClassNotFoundException
     * @throws ObjectBeanManagerException
     */
    public static HippoBean getHippoBean(String path) throws ClassNotFoundException, ObjectBeanManagerException {
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(PlayHippoTool.session, objectConverter);

        return (HippoBean) obm.getObject(path);
    }

    /**
     * Create a hst query
     * @param folderPath the content root
     * @return the hst query
     * @throws QueryException
     * @throws ObjectBeanManagerException
     * @throws ClassNotFoundException
     */
    public static HstQuery createQuery(String folderPath) throws QueryException, ObjectBeanManagerException, ClassNotFoundException {
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(PlayHippoTool.session, objectConverter);

        HstQueryManager queryManager = new HstQueryManagerImpl(PlayHippoTool.session, objectConverter, null);

        HippoFolder folder = (HippoFolder) obm.getObject(folderPath);
        return queryManager.createQuery(folder);
    }

    /**
     * Create a hippo folder.
     * @param newFolderNodePath the folder node path
     * @param hippoStdFolderNodeType the folder node type
     * @param folderName the folder name
     * @return the hippo folder
     * @throws ClassNotFoundException
     * @throws ObjectBeanManagerException
     */
    public static HippoFolderBean createFolder(String newFolderNodePath, String hippoStdFolderNodeType, String folderName)
            throws ClassNotFoundException, ObjectBeanManagerException {
        ObjectConverter objectConverter = getObjectConverter();

        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, objectConverter);

        // create a document with type and name
        String absoluteCreatedDocumentPath = wpm.createAndReturn(newFolderNodePath, hippoStdFolderNodeType, folderName, true);

        // retrieves the document created just before
        HippoFolderBean newFolder = (HippoFolderBean) wpm.getObject(absoluteCreatedDocumentPath);

        wpm.save();

        return newFolder;
    }

    /**
     * Create a document
     * @param folderNodePath the folder path
     * @param documentType the document type
     * @param newDocumentNodeName the document node name
     * @return created hippo document
     * @throws ClassNotFoundException
     * @throws ObjectBeanManagerException
     */
    public static HippoBean createDocument(String folderNodePath, String documentType, String newDocumentNodeName)
            throws ClassNotFoundException, ObjectBeanManagerException {
        ObjectConverter objectConverter = getObjectConverter();

        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, objectConverter);

        // create a document with type and name
        String absoluteCreatedDocumentPath = wpm.createAndReturn(folderNodePath, documentType, newDocumentNodeName, false);

        // retrieves the document created just before
        return (HippoBean) wpm.getObject(absoluteCreatedDocumentPath);
    }

    /**
     * Get the jcr session and access the native jcr api.
     * @return the session
     */
    public static Session getSession() {
        return session;
    }

}

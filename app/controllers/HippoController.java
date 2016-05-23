package controllers;

import model.HippoGoGreenNewsDocument;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetNavigation;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import org.onehippo.playhippo.services.PlayHippo;

import javax.jcr.*;
import java.util.*;


public class HippoController extends Controller {

    /**
     * Get jcr uuid and path from a jcr node by uuid.
     *
     * @param uuid
     * @return
     */
    public Result uuid(String uuid) {
        try {
            HstQuery hstQuery = PlayHippo.createQuery("/content/documents");

            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("jcr:uuid", uuid);
            hstQuery.setFilter(filter);

            final HstQueryResult result = hstQuery.execute();
            HippoBeanIterator it = result.getHippoBeans();

            final HippoBean bean;
            if (it.hasNext() && (bean = it.nextHippoBean()) != null) {
                return ok(Json.toJson(new HashMap() {
                    {
                        put("uuid", bean.getCanonicalUUID());
                        put("path", bean.getPath());
                    }
                }));
            }

            return notFound();
        } catch (ObjectBeanManagerException e) {
            Logger.error("Exception occurred, folder / unavailable.", e);
            return internalServerError(e.getMessage());
        } catch (QueryException e) {
            Logger.error("Exception occurred, HstQuery unavailable.", e);
            return internalServerError(e.getMessage());
        } catch (ClassNotFoundException e) {
            Logger.error("Exception occurred, HippoBean unavailable.", e);
            return internalServerError(e.getMessage());
        }
    }

    /**
     * Browse the jcr nodes by path.
     *
     * @param path the jcr path
     * @return node (properties, childs)
     */
    public Result browseJcr(String path) {
        StringBuilder pathBuilder = new StringBuilder("/").append(path);

        Session session = PlayHippo.getSession();
        try {
            Node root = session.getNode(pathBuilder.toString());

            Map<String, Object> properties = new HashMap<String, Object>();

            PropertyIterator propertyIterator = root.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                if (property.isMultiple()) {
                    List<String> multiValue = new LinkedList<String>();
                    for (Value value : property.getValues()) {
                        multiValue.add(value.toString());
                    }
                    properties.put(property.getName(), multiValue);
                } else {
                    properties.put(property.getName(), property.getValue().toString());
                }
            }

            Map<String, String> childs = new HashMap<String, String>();

            NodeIterator nodes = root.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                childs.put(node.getName(), node.getPath());
            }

            Map<String, Map> map = new HashMap<String, Map>();
            map.put("child", childs);
            map.put("properties", properties);

            return ok(Json.toJson(map));
        } catch (RepositoryException e) {
            Logger.error("Exception occurred, browsing repository.", e);
            return internalServerError(e.getMessage());
        }
    }

    /**
     * Hippo Content Example (Documents, Folders)
     *
     * @param path the content path
     * @return view template example
     */
    public Result content(String path) {
        StringBuilder pathBuilder = new StringBuilder("/content/").append(path);

        try {
            HippoBean hippoBean = PlayHippo.getHippoBean(pathBuilder.toString());
            if (hippoBean == null) {
                return noContent();
            }

            if (hippoBean instanceof HippoGoGreenNewsDocument) {
                Logger.info("title: "+((HippoGoGreenNewsDocument) hippoBean).getTitle());

                return ok(views.html.content.news.render((HippoGoGreenNewsDocument)hippoBean));
            } else if (hippoBean instanceof HippoDocument) {
                return ok(views.html.content.document.render((HippoDocument) hippoBean));
            } else if (hippoBean instanceof HippoFacetNavigation) {
                return ok(views.html.content.facet.render((HippoFacetNavigation)hippoBean));
            } else if (hippoBean instanceof HippoFolder) {
                return ok(views.html.content.folder.render((HippoFolder)hippoBean));
            }
        } catch (ClassNotFoundException e) {
            Logger.error("Exception occurred, browsing repository.", e);
        } catch (ObjectBeanManagerException e) {
            Logger.error("Exception occurred, browsing repository.", e);
        }

        return notFound();
    }

}

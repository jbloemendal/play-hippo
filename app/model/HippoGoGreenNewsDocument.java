/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package model;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="gogreen:newsdocument")
public class HippoGoGreenNewsDocument extends HippoDocument {
    
    protected String title;
    protected String bodyContent;

    public String getTitle() {
        if (title == null) {
            title = (String) getProperty("gogreen:title");
        }
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public HippoHtml getBody(){
        return getHippoHtml("gogreen:content");
    }
    
    public String getBodyContent() {
        if (bodyContent == null) {
            HippoHtml html = getBody();
            
            if (html == null) {
                return null;
            } else {
                return html.getContent();
            }
        } else {
            return bodyContent;
        }
    }
    
    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }
}

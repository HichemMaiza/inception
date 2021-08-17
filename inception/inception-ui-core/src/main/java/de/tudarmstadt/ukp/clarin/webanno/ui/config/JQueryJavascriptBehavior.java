/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.config;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.settings.JavaScriptLibrarySettings;

public class JQueryJavascriptBehavior
    extends Behavior
{
    private static final long serialVersionUID = -41338584738835064L;

    private static final JQueryJavascriptBehavior INSTANCE = new JQueryJavascriptBehavior();

    public static JQueryJavascriptBehavior get()
    {
        return INSTANCE;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        // We also load the JQuery CSS always just to get a consistent look across
        // the app
        JavaScriptLibrarySettings cfg = aComponent.getApplication().getJavaScriptLibrarySettings();

        if (cfg.getJQueryReference() != null) {
            aResponse.render(new PriorityHeaderItem(
                    JavaScriptHeaderItem.forReference(cfg.getJQueryReference())));
        }
    }
}

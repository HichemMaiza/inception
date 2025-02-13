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
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import static de.tudarmstadt.ukp.inception.active.learning.sidebar.ActiveLearningUserStateMetaData.CURRENT_AL_USER_STATE;

import java.util.Set;

import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionStartedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class ActiveLearningSidebarIcon
    extends Panel
{
    private static final long serialVersionUID = -1870047500327624860L;

    private @SpringBean ActiveLearningService activeLearningService;
    private @SpringBean UserDao userService;

    public ActiveLearningSidebarIcon(String aId, IModel<AnnotatorState> aState)
    {
        super(aId, aState);

        setOutputMarkupId(true);

        queue(new Icon("icon", FontAwesome5IconType.robot_s));
        queue(new Icon("badge", LoadableDetachableModel.of(this::getStateIcon))
                .add(new ClassAttributeModifier()
                {
                    private static final long serialVersionUID = 4534226094224688646L;

                    @Override
                    protected Set<String> update(Set<String> aClasses)
                    {
                        if (isSessionActive()) {
                            aClasses.add("text-primary");
                            aClasses.remove("text-muted");
                        }
                        else {
                            aClasses.add("text-muted");
                            aClasses.remove("text-primary");
                        }

                        return aClasses;
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    private boolean isSessionActive()
    {
        var alState = getPage().getMetaData(CURRENT_AL_USER_STATE);
        return alState != null && alState.isSessionActive();
    }

    private IconType getStateIcon()
    {
        if (isSessionActive()) {
            return FontAwesome5IconType.play_circle_s;
        }

        return FontAwesome5IconType.stop_circle_s;
    }

    @OnEvent
    public void sessionStarted(ActiveLearningSessionStartedEvent aEvent)
    {
        aEvent.getRequestTarget().add(this);
    }

    @OnEvent
    public void sessionStarted(ActiveLearningSessionCompletedEvent aEvent)
    {
        aEvent.getRequestTarget().add(this);
    }
}

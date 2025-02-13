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
package de.tudarmstadt.ukp.inception.workload.matrix.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogContent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish.FinishDocumentDialogModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class MatrixWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean MatrixWorkloadExtension matrixWorkloadExtension;
    private @SpringBean PreferencesService preferencesService;

    private final AnnotationPageBase page;
    protected ModalDialog finishDocumentDialog;
    private final ChallengeResponseDialog resetDocumentDialog;
    private final LambdaAjaxLink resetDocumentLink;
    private final IModel<MatrixWorkloadTraits> traits;

    public MatrixWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        traits = LoadableDetachableModel.of(() -> matrixWorkloadExtension
                .readTraits(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(
                        page.getModelObject().getProject())));

        finishDocumentDialog = new BootstrapModalDialog("finishDocumentDialog");
        finishDocumentDialog.setContent(new FinishDocumentDialogContent(ModalDialog.CONTENT_ID,
                Model.of(new FinishDocumentDialogModel()),
                this::actionFinishDocumentDialogSubmitted));
        add(finishDocumentDialog);

        add(createToggleDocumentStateLink("toggleDocumentState"));

        IModel<String> documentNameModel = PropertyModel.of(page.getModel(), "document.name");
        resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog");
        resetDocumentDialog.setTitleModel(new ResourceModel("ResetDocumentDialog.title"));
        resetDocumentDialog.setMessageModel(new ResourceModel("ResetDocumentDialog.text"));
        resetDocumentDialog.setExpectedResponseModel(documentNameModel);
        resetDocumentDialog.setConfirmAction(this::actionResetDocument);
        add(resetDocumentDialog);

        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                resetDocumentDialog::show));
        resetDocumentLink.add(enabledWhen(() -> page.isEditable()));
        resetDocumentLink.add(visibleWhen(
                traits.map(MatrixWorkloadTraits::isDocumentResetAllowed).orElse(false)));
    }

    private LambdaAjaxLink createToggleDocumentStateLink(String aId)
    {
        LambdaAjaxLink link;
        if (isReopenableByUser()) {
            link = new LambdaAjaxLink(aId, this::actionToggleDocumentState);
        }
        else {
            link = new LambdaAjaxLink(aId, this::actionRequestFinishDocumentConfirmation);
        }
        link.setOutputMarkupId(true);
        link.add(enabledWhen(() -> page.isEditable() || isReopenableByUser()));
        var stateLabel = new Label("state");
        stateLabel.add(new CssClassNameModifier(LoadableDetachableModel.of(this::getStateClass)));
        stateLabel.add(AttributeModifier.replace("title", LoadableDetachableModel.of(() -> {
            var tooltip = this.getStateTooltip();
            return tooltip.wrapOnAssignment(stateLabel).getObject();
        })));
        link.add(stateLabel);
        return link;
    }

    private boolean isReopenableByUser()
    {
        // Curators can re-open documents anyway via the monitoring page, so we can always allow
        // the re-open documents here as well
        AnnotatorState state = page.getModelObject();
        if (projectService.hasRole(userRepository.getCurrentUsername(), state.getProject(),
                CURATOR)) {
            return true;
        }

        return traits.getObject().isReopenableByAnnotator();
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public ResourceModel getStateTooltip()
    {
        AnnotatorState state = page.getModelObject();

        // Curation sidebar: when writing to the curation document, we need to update the document
        if (state.getUser().getUsername().equals(CURATION_USER)) {
            if (state.getDocument().getState() == SourceDocumentState.CURATION_FINISHED) {
                return new ResourceModel("stateToggle.curationFinished");
            }
            else {
                return new ResourceModel("stateToggle.curationInProgress");
            }
        }

        if (documentService.isAnnotationFinished(state.getDocument(), state.getUser())) {
            return new ResourceModel("stateToggle.annotationFinished");
        }
        else {
            return new ResourceModel("stateToggle.annotationInProgress");
        }
    }

    public String getStateClass()
    {
        AnnotatorState state = page.getModelObject();

        // Curation sidebar: when writing to the curation document, we need to update the document
        if (state.getUser().getUsername().equals(CURATION_USER)) {
            if (state.getDocument().getState() == SourceDocumentState.CURATION_FINISHED) {
                // SourceDocumentState.CURATION_FINISHED.symbol()
                return FontAwesome5IconType.clipboard_check_s.cssClassName();
            }
            else {
                // SourceDocumentState.CURATION_IN_PROGRESS.symbol()
                return FontAwesome5IconType.clipboard_s.cssClassName();
            }
        }

        if (documentService.isAnnotationFinished(state.getDocument(), state.getUser())) {
            // AnnotationDocumentState.FINISHED.symbol();
            return FontAwesome5IconType.lock_s.cssClassName();
        }
        else {
            // AnnotationDocumentState.IN_PROGRESS.symbol();
            return FontAwesome5IconType.lock_open_s.cssClassName();
        }
    }

    protected void actionRequestFinishDocumentConfirmation(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        try {
            page.actionValidateDocument(aTarget, page.getEditorCas());
        }
        catch (ValidationException e) {
            page.error("Document cannot be marked as finished: " + e.getMessage());
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        finishDocumentDialog.open(aTarget);
    }

    private void actionFinishDocumentDialogSubmitted(AjaxRequestTarget aTarget,
            Form<FinishDocumentDialogModel> aForm)
    {
        AnnotatorState state = page.getModelObject();

        var newState = aForm.getModelObject().getState();

        AnnotationDocument annotationDocument = documentService
                .getAnnotationDocument(state.getDocument(), state.getUser());
        annotationDocument.setAnnotatorComment(aForm.getModelObject().getComment());
        documentService.setAnnotationDocumentState(annotationDocument, newState,
                EXPLICIT_ANNOTATOR_USER_ACTION);

        if (newState == AnnotationDocumentState.IGNORE) {
            state.reset();
            state.setDocument(null, null);
        }

        aTarget.add(page);
    }

    private void actionToggleDocumentState(AjaxRequestTarget aTarget)
    {
        // state instead
        AnnotatorState state = page.getModelObject();
        SourceDocument document = state.getDocument();

        // Curation sidebar: when writing to the curation document, we need to update the docuement
        if (state.getUser().getUsername().equals(CURATION_USER)) {
            switch (document.getState()) {
            case CURATION_FINISHED:
                documentService.setSourceDocumentState(document, CURATION_IN_PROGRESS);
                aTarget.add(page);
                break;
            default:
                documentService.setSourceDocumentState(document, CURATION_FINISHED);
                aTarget.add(page);
                break;
            }
            return;
        }

        var annDoc = documentService.getAnnotationDocument(document, state.getUser());
        if (annDoc.getAnnotatorState() != annDoc.getState()) {
            error("Annotation state has been overridden by a project manager or curator. "
                    + "You cannot change it.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // We look at the annotator state here because annotators should only be able to re-open
        // documents that they closed themselves - not documents that were closed e.g. by a manager
        var annState = annDoc.getAnnotatorState();
        switch (annState) {
        case IN_PROGRESS:
            documentService.setAnnotationDocumentState(annDoc, FINISHED,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
            aTarget.add(page);
            break;
        case FINISHED:
            documentService.setAnnotationDocumentState(annDoc, IN_PROGRESS,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
            aTarget.add(page);
            break;
        default:
            error("Can only change document state for documents that are finished or in progress, "
                    + "but document is in state [" + annState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }
    }

    protected void actionResetDocument(AjaxRequestTarget aTarget) throws Exception
    {
        AnnotatorState state = page.getModelObject();
        documentService.resetAnnotationCas(state.getDocument(), state.getUser(),
                EXPLICIT_ANNOTATOR_USER_ACTION);
        page.actionLoadDocument(aTarget);
    }
}

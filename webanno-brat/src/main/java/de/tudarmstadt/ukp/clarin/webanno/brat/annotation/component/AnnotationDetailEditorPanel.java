/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getNextSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceBeginAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.setFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.codehaus.plexus.util.StringUtils;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.command.Selection;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.JavascriptUtils;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior2;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Annotation Detail Editor Panel.
 *
 */
public class AnnotationDetailEditorPanel
    extends Panel
{
    private static final long serialVersionUID = 7324241992353693848L;
    private static final Log LOG = LogFactory.getLog(AnnotationDetailEditorPanel.class);

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private AnnotationFeatureForm annotationFeatureForm;
    private Label selectedTextLabel;
    private CheckBox forwardAnnotationCheck;
    private RefreshingView<FeatureModel> featureValues;

    private AjaxButton deleteButton;
    private AjaxButton reverseButton;

    private LayerSelector layerSelector;   
    private TextField<String> forwardAnnotationText;
    private Label selectedAnnotationLayer;
    private ModalWindow deleteModal;

    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    private List<FeatureModel> featureModels;
    private BratAnnotatorModel bModel;
    private String selectedTag = "";
    
    /**
     * Function to return tooltip using jquery
     * Docs for the JQuery tooltip widget that we configure below:
     * https://api.jqueryui.com/tooltip/
     */
    private final String functionForTooltip = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";

    public AnnotationDetailEditorPanel(String id, IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        
        bModel = aModel.getObject();
        featureModels = new ArrayList<>();          

        annotationFeatureForm = new AnnotationFeatureForm("annotationFeatureForm",
                aModel.getObject())
        {
            private static final long serialVersionUID = 8081614428845920047L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                // Avoid reversing in read-only layers
                setEnabled(bModel.getDocument() != null && !isAnnotationFinished());
            }
        };

        annotationFeatureForm.setOutputMarkupId(true);
        annotationFeatureForm.add(new AjaxFormValidatingBehavior(annotationFeatureForm, "submit") { 
			private static final long serialVersionUID = -5642108496844056023L;

			@Override 
            protected void onSubmit(AjaxRequestTarget aTarget) { 
               try {
                   actionAnnotate(aTarget, bModel, false);
    			} catch (UIMAException | ClassNotFoundException | IOException | BratAnnotationException e) {
    				error(e.getMessage());
    			}
            } 

        }); 
        add(annotationFeatureForm);
    }

    public boolean isAnnotationFinished()
    {
        if (bModel.getMode().equals(Mode.CURATION)) {
            return bModel.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED);

        }
        else {
            return repository.getAnnotationDocument(bModel.getDocument(), bModel.getUser())
                    .getState().equals(AnnotationDocumentState.FINISHED);
        }
    }

    private class AnnotationFeatureForm
        extends Form<BratAnnotatorModel>
    {
        private static final long serialVersionUID = 3635145598405490893L;
        private WebMarkupContainer featureEditorsContainer;

        public AnnotationFeatureForm(String id, BratAnnotatorModel aBModel)
        {
            super(id, new CompoundPropertyModel<BratAnnotatorModel>(aBModel));

            add(forwardAnnotationCheck = new CheckBox("forwardAnnotation")
            {
                private static final long serialVersionUID = 8908304272310098353L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    setEnabled(isForwardable());
                    updateForwardAnnotation(bModel);

                }
            });
            forwardAnnotationCheck.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    updateForwardAnnotation(getModelObject());
                    if(bModel.isForwardAnnotation()){
                    	aTarget.appendJavaScript(JavascriptUtils.getFocusScript(forwardAnnotationText));
                    	selectedTag = "";
                    }
                }
            });

            forwardAnnotationCheck.setOutputMarkupId(true);

            add(new Label("noAnnotationWarning", "No annotation selected!"){

                private static final long serialVersionUID = -6046409838139863541L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(!bModel.getSelection().getAnnotation().isSet());
                }
                 
            });

            add(deleteButton = new AjaxButton("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getSelection().getAnnotation().isSet());

                    // Avoid deleting in read-only layers
                    setEnabled(bModel.getSelectedAnnotationLayer() != null
                            && !bModel.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    try {
                        JCas jCas = getCas(bModel);
                        AnnotationFS fs = selectByAddr(jCas, bModel.getSelection().getAnnotation().getId());

                        AnnotationLayer layer = bModel.getSelectedAnnotationLayer();
                        TypeAdapter adapter = getAdapter(annotationService, layer);
                        if (adapter instanceof SpanAdapter && getAttachedRels(jCas, fs, layer).size() > 0) {
                            deleteModal.setTitle("Are you sure you like to delete all attached relations to this span annotation?");
                            deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel(
                                    deleteModal.getContentId(), bModel, deleteModal,
                                    AnnotationDetailEditorPanel.this,
                                    bModel.getSelectedAnnotationLayer(), false));
                            deleteModal.show(aTarget);
                        }
                        else {
                            actionDelete(aTarget, bModel);
                        }
                    }
                    catch (UIMAException | ClassNotFoundException | IOException
                            | CASRuntimeException | BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
            });

            add(reverseButton = new AjaxButton("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getSelection().isRelationAnno()
                            && bModel.getSelection().getAnnotation().isSet()
                            && bModel.getSelectedAnnotationLayer().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE));

                    // Avoid reversing in read-only layers
                    setEnabled(bModel.getSelectedAnnotationLayer() != null
                            && !bModel.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    try {
                        actionReverse(aTarget, bModel);
                    }
                    catch (BratAnnotationException e) {
                        aTarget.prependJavaScript("alert('" + e.getMessage() + "')");
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(e.getMessage());
                        LOG.error(e.getMessage(), e);
                    }
                }
            });
            reverseButton.setOutputMarkupPlaceholderTag(true);
            
            add(new AjaxButton("clear")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getSelection().getAnnotation().isSet());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    try {
                        actionClear(aTarget, bModel);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(e.getMessage());
                        LOG.error(e.getMessage(), e);
                    }
                }
            });

            add(layerSelector = new LayerSelector("defaultAnnotationLayer", annotationLayers));

            featureEditorsContainer = new WebMarkupContainer("featureEditorsContainer")
            {
                private static final long serialVersionUID = 8908304272310098353L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getSelection().getAnnotation().isSet());
                }
            };
            // Add placeholder since wmc might start out invisible. Without the placeholder we
            // cannot make it visible in an AJAX call
            featureEditorsContainer.setOutputMarkupPlaceholderTag(true);
            featureEditorsContainer.setOutputMarkupId(true);
            
            featureEditorsContainer.add(new Label("noFeaturesWarning", "No features available!") {
                private static final long serialVersionUID = 4398704672665066763L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(featureModels.isEmpty());
                }
            });
            
            featureValues = new FeatureEditorPanelContent("featureValues");
            featureEditorsContainer.add(featureValues);
            
			forwardAnnotationText = new TextField<String>("forwardAnno");
			forwardAnnotationText.setOutputMarkupId(true);
			forwardAnnotationText.add(new AjaxFormComponentUpdatingBehavior("keyup") {
				private static final long serialVersionUID = 4554834769861958396L;
				
				   @Override
			        protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
			            super.updateAjaxAttributes(attributes);

			            IAjaxCallListener listener = new AjaxCallListener(){
							private static final long serialVersionUID = -7968540662654079601L;

							@Override
			                public CharSequence getPrecondition(Component component) {
			                    return  "var keycode = Wicket.Event.keyCode(attrs.event);" +			    
			                            "    return true;" ;
			                }
			            };
			            attributes.getAjaxCallListeners().add(listener);

			            attributes.getDynamicExtraParameters()
			                .add("var eventKeycode = Wicket.Event.keyCode(attrs.event);" +
			                     "return {keycode: eventKeycode};");
			            attributes.setAllowDefault(true);
			        }
				   
				@Override
                protected void onUpdate(AjaxRequestTarget aTarget) {	
					final Request request = RequestCycle.get().getRequest();
		            final String jsKeycode = request.getRequestParameters()
		                            .getParameterValue("keycode").toString("");
		            if (jsKeycode.equals("32")){
		            	try {
							actionAnnotate(aTarget, aBModel, false);
							selectedTag ="";
						} catch (UIMAException | ClassNotFoundException | IOException | BratAnnotationException e) {
						error(e);
						}
		            	return;
		            }
		            if (jsKeycode.equals("13")){
		            	selectedTag ="";
		            	return;
		            }
					selectedTag = (forwardAnnotationText.getModelObject() == null ? ""
							: forwardAnnotationText.getModelObject().charAt(0)) + selectedTag;
					Map<String, String> bindTags = getBindTags();
					if (!bindTags.isEmpty()) {
					    featureModels.get(0).value = getKeyBindValue(selectedTag, bindTags);
					}
					aTarget.add(forwardAnnotationText);
					aTarget.add(featureValues.get(0));
				}
			});
            forwardAnnotationText.setOutputMarkupId(true);
            forwardAnnotationText.add(new AttributeAppender("style", "opacity:0", ";"));
            // forwardAnno.add(new AttributeAppender("style", "filter:alpha(opacity=0)", ";"));
            add(forwardAnnotationText);
            
            // the selected text for annotation
            selectedTextLabel = new Label("selectedText", PropertyModel.of(getModelObject(),
                    "selection.text"));
            selectedTextLabel.setOutputMarkupId(true);
            featureEditorsContainer.add(selectedTextLabel);
            
            featureEditorsContainer.add(new Label("layerName","Layer"){
                private static final long serialVersionUID = 6084341323607243784L;
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getPreferences().isRememberLayer());
                }
                
            });
            featureEditorsContainer.setOutputMarkupId(true);

            // the annotation layer for the selected annotation
            selectedAnnotationLayer = new Label("selectedAnnotationLayer", new Model<String>())
            {
                private static final long serialVersionUID = 4059460390544343324L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getPreferences().isRememberLayer());
                }

            };
            selectedAnnotationLayer.setOutputMarkupId(true);
            featureEditorsContainer.add(selectedAnnotationLayer);
            
            add(featureEditorsContainer);
            
            add(deleteModal = new ModalWindow("yesNoModal"));
            deleteModal.setOutputMarkupId(true);

            deleteModal.setInitialWidth(600);
            deleteModal.setInitialHeight(50);
            deleteModal.setResizable(true);
            deleteModal.setWidthUnit("px");
            deleteModal.setHeightUnit("px");
            deleteModal.setTitle("Are you sure you want to delete the existing annotation?");
        }
    }

    public void actionAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel, boolean aIsForwarded)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
		if (isAnnotationFinished()) {
			throw new BratAnnotationException(
					"This document is already closed. Please ask your project manager to re-open it via the Monitoring page");
		}
        // If there is no annotation yet, create one. During creation, the adapter
        // may notice that it would create a duplicate and return the address of
        // an existing annotation instead of a new one.
        JCas jCas = getCas(aBModel);

        actionAnnotate(aTarget, aBModel, jCas, aIsForwarded);
    }

    public void actionAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel, JCas jCas, boolean aIsForwarded)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        if (aBModel.getSelectedAnnotationLayer() == null) {
            error("No layer is selected. First select a layer.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            return;
        }

        if (aBModel.getSelectedAnnotationLayer().isReadonly()) {
            error("Layer is not editable.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            return;
        }

        // Verify if input is valid according to tagset
        for (int i = 0; i < featureModels.size(); i++) {
            AnnotationFeature feature = featureModels.get(i).feature;
            if (CAS.TYPE_NAME_STRING.equals(feature.getType())) {
                String value = (String) featureModels.get(i).value;
            	
                // Check if tag is necessary, set, and correct
                if (
                    value != null &&
                    feature.getTagset() != null && 
                    !feature.getTagset().isCreateTag() && 
                    !annotationService.existsTag(value, feature.getTagset())
                ) {
                    error("[" + value
                            + "] is not in the tag list. Please choose from the existing tags");
                    return;
                }
            }
        }
        
        // #186 - After filling a slot, the annotation detail panel is not updated 
        aTarget.add(annotationFeatureForm);
        
		TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());
		Selection selection = aBModel.getSelection();
		if (selection.getAnnotation().isNotSet()) {
			if (bModel.getSelection().isRelationAnno()) {
				AnnotationFS originFs = selectByAddr(jCas, selection.getOrigin());
				AnnotationFS targetFs = selectByAddr(jCas, selection.getTarget());
				if (adapter instanceof SpanAdapter) {
					error("Layer do not support arc annotation.");
		            aTarget.addChildren(getPage(), FeedbackPanel.class);
		            return;
				}
				if (adapter instanceof ArcAdapter) {
					Sentence sentence = selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
							bModel.getSentenceEndOffset());
					int start = sentence.getBegin();
					int end = selectByAddr(jCas, Sentence.class, getLastSentenceAddressInDisplayWindow(jCas,
							getAddr(sentence), bModel.getPreferences().getWindowSize())).getEnd();

					AnnotationFS arc = ((ArcAdapter) adapter).add(originFs, targetFs, jCas, start, end, null, null);
					selection.setAnnotation(new VID(getAddr(arc)));
                    if (selection.getAnnotation().isSet()) {
                        selection.setText("[" + originFs.getCoveredText() + "] - [" + 
                                targetFs.getCoveredText() + "]");
                    }
                    else {
                        selection.setText("");
                    }
				} else {
					selection.setAnnotation(
							new VID(((ChainAdapter) adapter).addArc(jCas, originFs, targetFs, null, null)));
					if (selection.getAnnotation().isSet()) {
					    selection.setText(originFs.getCoveredText());
					}
					else {
					    selection.setText("");
					}
				}
				selection.setBegin(originFs.getBegin());
			} else if (adapter instanceof SpanAdapter) {
				
				for (FeatureModel fm : featureModels) {
					Serializable spanValue = ((SpanAdapter) adapter).getSpan(jCas, selection.getBegin(),
							selection.getEnd(), fm.feature, null);
					if (spanValue != null) {
						// allow modification for forward annotation
						if (aBModel.isForwardAnnotation()) {
							fm.value = spanValue;	
							featureModels.get(0).value = spanValue;
							selectedTag = 
									getBindTags().entrySet().stream().filter(e -> e.getValue().equals(spanValue))
									.map(Map.Entry::getKey).findFirst().orElse(null);
						} else {
							actionClear(aTarget, bModel);
							throw new BratAnnotationException("Cannot create another annotation of layer ["
									+ bModel.getSelectedAnnotationLayer().getUiName() + "] at this"
									+ " location - stacking is not enabled for this layer.");
						}
					}
				}
				Integer annoId = ((SpanAdapter) adapter).add(jCas, selection.getBegin(), selection.getEnd(), null, null);
				selection.setAnnotation(new VID(annoId));
				AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(jCas, annoId);
				selection.set(jCas, annoFs.getBegin(), annoFs.getEnd());
			} else {

				for (FeatureModel fm : featureModels) {
					Serializable spanValue = ((ChainAdapter) adapter).getSpan(jCas, selection.getBegin(),
							selection.getEnd(), fm.feature, null);
					if (spanValue != null) {
						// allow modification for forward annotation
						if (aBModel.isForwardAnnotation()) {
							fm.value = spanValue;
							featureModels.get(0).value = spanValue;
							selectedTag = 
									getBindTags().entrySet().stream().filter(e -> e.getValue().equals(spanValue))
									.map(Map.Entry::getKey).findFirst().orElse(null);
						} 
					}
				}
				selection.setAnnotation(new VID(((ChainAdapter) adapter).addSpan(
				        jCas, selection.getBegin(), selection.getEnd(), null, null)));
                selection.setText(jCas.getDocumentText().substring(
                        selection.getBegin(), selection.getEnd()));
			}
		}

        // Set feature values
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (FeatureModel fm : featureModels) {
            features.add(fm.feature);

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(fm.feature.getType())) {
                String value = (String) fm.value;

                if (
                    value != null &&
                    fm.feature.getTagset() != null && 
                    fm.feature.getTagset().isCreateTag() && 
                    !annotationService.existsTag(value, fm.feature.getTagset())
                ) {
                    Tag selectedTag = new Tag();
                    selectedTag.setName(value);
                    selectedTag.setTagSet(fm.feature.getTagset());
                    annotationService.createTag(selectedTag, aBModel.getUser());
                }
            }
            adapter.updateFeature(jCas, fm.feature, aBModel.getSelection().getAnnotation().getId(),
                    fm.value);
        }

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, aBModel.getSelection().getBegin());
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // persist changes
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        if (bModel.getSelection().isRelationAnno()) {
            aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedArcFeatures(featureModels);
        }
        else {
            aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedSpanFeatures(featureModels);
        }

		aBModel.getSelection().setAnnotate(true);
		if (aBModel.getSelection().getAnnotation().isSet()) {
			String bratLabelText = TypeUtil.getBratLabelText(adapter,
					selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId()), features);
			info(generateMessage(aBModel.getSelectedAnnotationLayer(), bratLabelText, false));
		}

		onAnnotate(aTarget, aBModel);

		if (aBModel.isForwardAnnotation() && !aIsForwarded && featureModels.get(0).value != null) {
			if (aBModel.getSelection().getEnd() >= aBModel.getSentenceEndOffset()) {
				autoForwardScroll(jCas, aBModel);
			}
			onAutoForward(aTarget, aBModel);

		} else if (aBModel.getPreferences().isScrollPage()) {
			autoScroll(jCas, aBModel);
		}
		forwardAnnotationText.setModelObject(null);
		onChange(aTarget, aBModel);
		if (aBModel.isForwardAnnotation() && featureModels.get(0).value != null) {
			aTarget.add(annotationFeatureForm);
		}
	}
    
    public void actionDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, CASRuntimeException,
        BratAnnotationException
    {
        JCas jCas = getCas(aBModel);
        AnnotationFS fs = selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId());

        // TODO We assume here that the selected annotation layer corresponds to the type of the
        // FS to be deleted. It would be more robust if we could get the layer from the FS itself.
        AnnotationLayer layer = aBModel.getSelectedAnnotationLayer();
        TypeAdapter adapter = getAdapter(annotationService, layer);

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        //
        // NOTE: It is important that this happens before UNATTACH SPANS since the attach feature
        // is no longer set after UNATTACH SPANS!
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFS attachedFs : getAttachedRels(jCas, fs, layer)) {
                jCas.getCas().removeFsFromIndexes(attachedFs);
                info("The attached annotation for relation type [" + annotationService
                        .getLayer(attachedFs.getType().getName(), bModel.getProject()).getUiName()
                        + "] is deleted");
            }
        }

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.

        // == UNATTACH SPANS ==
        // If the deleted FS is a span that is attached to another span, the
        // attachFeature in the other span must be set to null. Typical example: POS is deleted, so
        // the pos feature of Token must be set to null. This is a quick case, because we only need
        // to look at span annotations that have the same offsets as the FS to be deleted.
        if (adapter instanceof SpanAdapter && layer.getAttachType() != null) {
            Type spanType = CasUtil.getType(jCas.getCas(), layer.getAttachType().getName());
            Feature attachFeature = spanType.getFeatureByBaseName(layer.getAttachFeature()
                    .getName());

            for (AnnotationFS attachedFs : selectAt(jCas.getCas(), spanType, fs.getBegin(),
                    fs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), fs)) {
                    attachedFs.setFeatureValue(attachFeature, null);
                    LOG.debug("Unattached [" + attachFeature.getShortName() + "] on annotation ["
                            + getAddr(attachedFs) + "]");
                }
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService.listAttachedLinkFeatures(layer)) {
                Type linkType = CasUtil.getType(jCas.getCas(), linkFeature.getLayer().getName());

                for (AnnotationFS linkFS : CasUtil.select(jCas.getCas(), linkType)) {
                    List<LinkWithRoleModel> links = getFeature(linkFS, linkFeature);
                    Iterator<LinkWithRoleModel> i = links.iterator();
                    boolean modified = false;
                    while (i.hasNext()) {
                        LinkWithRoleModel link = i.next();
                        if (link.targetAddr == getAddr(fs)) {
                            i.remove();
                            LOG.debug("Cleared slot [" + link.role + "] in feature ["
                                    + linkFeature.getName() + "] on annotation [" + getAddr(linkFS)
                                    + "]");
                            modified = true;
                        }
                    }
                    if (modified) {
                        setFeature(linkFS, linkFeature, links);
                    }
                }
            }
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof ArcAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        adapter.delete(jCas, aBModel.getSelection().getAnnotation());

        // Store CAS again
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, aBModel.getSelection().getBegin());
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // Auto-scroll
        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.getSelection().setAnnotate(false);

        info(generateMessage(aBModel.getSelectedAnnotationLayer(), null, true));

        // A hack to remember the visual DropDown display value
        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedSpanFeatures(featureModels);

        aBModel.getSelection().clear();

        // after delete will follow annotation
        bModel.getSelection().setAnnotate(true);
        aTarget.add(annotationFeatureForm);

        aTarget.add(deleteButton);
        aTarget.add(reverseButton);
        onChange(aTarget, aBModel);
        onDelete(aTarget, aBModel, fs);
    }

    private void actionReverse(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas jCas;
        jCas = getCas(aBModel);

        AnnotationFS idFs = selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId());

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, aBModel.getSelection().getOrigin());
        AnnotationFS targetFs = selectByAddr(jCas, aBModel.getSelection().getTarget());

        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());
        Sentence sentence = selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        int start = sentence.getBegin();
        int end = selectByAddr(jCas,
                Sentence.class, getLastSentenceAddressInDisplayWindow(jCas,
                        getAddr(sentence), bModel.getPreferences().getWindowSize()))
                                .getEnd();
        if (adapter instanceof ArcAdapter) {
            if(featureModels.size()==0){
                //If no features, still create arc #256
                AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas, start, end,
                        null, null);
                    aBModel.getSelection().setAnnotation(new VID(getAddr(arc)));
            }
            else{
                for (FeatureModel fm : featureModels) {
                    AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas, start, end,
                        fm.feature, fm.value);
                    aBModel.getSelection().setAnnotation(new VID(getAddr(arc)));
                }
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);
        int sentenceNumber = getSentenceNumber(jCas, originFs.getBegin());
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        info("The arc has been reversed");
        aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedArcFeatures(featureModels);

        // in case the user re-reverse it
        int temp = aBModel.getSelection().getOrigin();
        aBModel.getSelection().setOrigin(aBModel.getSelection().getTarget());
        aBModel.getSelection().setTarget(temp);

        onChange(aTarget, aBModel);
    }

    public  void actionClear(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        reset(aTarget);
        aTarget.add(annotationFeatureForm);
        onChange(aTarget, aBModel);
    }
    
    public JCas getCas(BratAnnotatorModel aBModel)
        throws UIMAException, IOException, ClassNotFoundException
    {

        if (aBModel.getMode().equals(Mode.ANNOTATION) || aBModel.getMode().equals(Mode.AUTOMATION)
                || aBModel.getMode().equals(Mode.CORRECTION)
                || aBModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readAnnotationCas(aBModel.getDocument(), aBModel.getUser());
        }
        else {
            return repository.readCurationCas(aBModel.getDocument());
        }
    }

    private void autoScroll(JCas jCas, BratAnnotatorModel aBModel)
    {
        int address = getAddr(selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset()));
        aBModel.setSentenceAddress(getSentenceBeginAddress(jCas, address, aBModel.getSelection()
                .getBegin(), aBModel.getProject(), aBModel.getDocument(), aBModel.getPreferences()
                .getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress());
        aBModel.setSentenceBeginOffset(sentence.getBegin());
        aBModel.setSentenceEndOffset(sentence.getEnd());

        Sentence firstSentence = selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(jCas, getAddr(firstSentence),
                aBModel.getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(jCas, FeatureStructure.class,
                lastAddressInPage);
        aBModel.setFSN(BratAjaxCasUtil.getSentenceNumber(jCas, firstSentence.getBegin()));
        aBModel.setLSN(BratAjaxCasUtil.getSentenceNumber(jCas, lastSentenceInPage.getBegin()));
    }

    private void autoForwardScroll(JCas jCas, BratAnnotatorModel aBModel)
    {
        int address = getNextSentenceAddress(jCas, selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress()));
        aBModel.setSentenceAddress(address);

        Sentence sentence = selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress());
        aBModel.setSentenceBeginOffset(sentence.getBegin());
        aBModel.setSentenceEndOffset(sentence.getEnd());

        Sentence firstSentence = selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(jCas, getAddr(firstSentence),
                aBModel.getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(jCas, FeatureStructure.class,
                lastAddressInPage);
        aBModel.setFSN(BratAjaxCasUtil.getSentenceNumber(jCas, firstSentence.getBegin()));
        aBModel.setLSN(BratAjaxCasUtil.getSentenceNumber(jCas, lastSentenceInPage.getBegin()));
    }
    
    @SuppressWarnings("unchecked")
    public void setSlot(AjaxRequestTarget aTarget, JCas aJCas, final BratAnnotatorModel aBModel,
            int aAnnotationId)
    {
        // Set an armed slot
        if (!bModel.getSelection().isRelationAnno() && aBModel.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getFeatureModel(aBModel
                    .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(aBModel.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
            aBModel.clearArmedSlot();
        }

        // Auto-commit if working on existing annotation
        if (bModel.getSelection().getAnnotation().isSet()) {
            try {
                actionAnnotate(aTarget, bModel, aJCas, false);
            }
            catch (BratAnnotationException e) {
                error(e.getMessage());
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }
            catch (Exception e) {
                error(ExceptionUtils.getRootCauseMessage(e));
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }
        }
    }

    private void arcSelected(AjaxRequestTarget aTarget, JCas aJCas) 
        throws BratAnnotationException
    {
        // FIXME REC I think this whole section which meddles around with the selected annotation
        // layer should be moved out of there to the place where we originally set the annotation
        // layer...!
        long layerId = TypeUtil.getLayerId(bModel.getSelection().getOriginType());
        AnnotationLayer spanLayer = annotationService.getLayer(layerId);
        if (
                bModel.getPreferences().isRememberLayer() && 
                bModel.getSelection().isAnnotate() && 
                !spanLayer.equals(bModel.getDefaultAnnotationLayer())) 
        {
            throw new BratAnnotationException("No relation annotation allowed on the "
                    + "selected span layer");
        }

        // If we are creating a relation annotation, we have to set the current layer depending
        // on the type of relation that is permitted between the source/target span. This is
        // necessary because we have no separate UI control to set the relation annotation type.
        // It is possible because currently only a single relation layer is allowed to attach to
        // any given span layer.
        if (bModel.getSelection().isAnnotate()) 
        {
            // If we drag an arc between POS annotations, then the relation must be a dependency
            // relation.
            // FIXME - Actually this case should be covered by the last case - the database lookup!
            if (
                    spanLayer.isBuiltIn() && 
                    spanLayer.getName().equals(POS.class.getName())) 
            {
                AnnotationLayer depLayer = annotationService.getLayer(Dependency.class.getName(),
                        bModel.getProject());
                if (bModel.getAnnotationLayers().contains(depLayer)) {
                    bModel.setSelectedAnnotationLayer(depLayer);
                }
                else {
                    bModel.setSelectedAnnotationLayer(null);
                }
            }
            // If we drag an arc in a chain layer, then the arc is of the same layer as the span
            // Chain layers consist of arcs and spans
            else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                // one layer both for the span and arc annotation
                bModel.setSelectedAnnotationLayer(spanLayer);
            }
            // Otherwise, look up the possible relation layer(s) in the database.
            else {
                for (AnnotationLayer layer : annotationService.listAnnotationLayer(bModel
                        .getProject())) {
                    if (layer.getAttachType() != null && layer.getAttachType().equals(spanLayer)) {
                        if (bModel.getAnnotationLayers().contains(layer)) {
                            bModel.setSelectedAnnotationLayer(layer);
                        }
                        else {
                            bModel.setSelectedAnnotationLayer(null);
                        }
                        break;
                    }
                }
            }
        }

        // Populate feature value from existing annotation
        if (bModel.getSelection().getAnnotation().isSet()) {
            AnnotationFS annoFs = selectByAddr(aJCas, bModel.getSelection().getAnnotation()
                    .getId());

            // Try obtaining the layer from the feature structure
            AnnotationLayer layer;
            try {
                layer = TypeUtil.getLayer(annotationService, bModel.getProject(), annoFs);
            }
            catch (NoResultException e) {
                clearFeatures(aTarget);
                throw new IllegalStateException("Unknown layer [" + annoFs.getType().getName() + "]", e);
            }
            
            populateFeatures(layer, annoFs, null);
        }
        // Avoid creation of arcs on locked layers
        else if (bModel.getSelectedAnnotationLayer() != null
                && bModel.getSelectedAnnotationLayer().isReadonly()) {
            bModel.setSelectedAnnotationLayer(new AnnotationLayer());
        }
        else {
            populateFeatures(bModel.getSelectedAnnotationLayer(), null, 
                    bModel.getRememberedArcFeatures());
        }
        
        bModel.setDefaultAnnotationLayer(spanLayer);
    }
    
    private void spanSelected(AjaxRequestTarget aTarget, JCas aJCas)
    {
        // Selecting an existing span annotation
        if (bModel.getSelection().getAnnotation().isSet()) {
            AnnotationFS annoFs = selectByAddr(aJCas, bModel.getSelection().getAnnotation()
                    .getId());
            // Try obtaining the layer from the feature structure
            AnnotationLayer layer;
            try {
                layer = TypeUtil.getLayer(annotationService, bModel.getProject(), annoFs);
            }
            catch (NoResultException e) {
                clearFeatures(aTarget);
                throw new IllegalStateException("Unknown layer [" + annoFs.getType().getName() + "]", e);
            }

            // If remember layer is off, then the current layer follows the selected annotations
            if (!bModel.getPreferences().isRememberLayer()) {
                bModel.setSelectedAnnotationLayer(layer);
            }
            
            // populate feature value
            populateFeatures(layer, annoFs, null);
        }
        else {
            populateFeatures(bModel.getSelectedAnnotationLayer(), null, 
                    bModel.getRememberedSpanFeatures());
        }        
    }

    protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
    {
        // Overriden in BratAnnotator
    }

    protected void onAutoForward(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
    {
        // Overriden in BratAnnotator
    }

    protected void onAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aModel)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aModel, AnnotationFS aFs)
    {
        // Overriden in AutomationPage
    }

    public void refreshAnnotationLayers(BratAnnotatorModel aBModel)
    {
        updateLayersDropdown(aBModel);
        if (annotationLayers.size() == 0) {
            aBModel.setSelectedAnnotationLayer(new AnnotationLayer());
        }
        else if (aBModel.getSelectedAnnotationLayer() == null) {
            if (aBModel.getRememberedSpanLayer() == null) {
                aBModel.setSelectedAnnotationLayer(annotationLayers.get(0));
            }
            else {
                aBModel.setSelectedAnnotationLayer(aBModel.getRememberedSpanLayer());
            }
        }
        clearFeatures(null);
        updateRememberLayer();
    }

    private void updateLayersDropdown(BratAnnotatorModel aBModel)
    {
        annotationLayers.clear();
        AnnotationLayer l = null;
        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            if (!layer.isEnabled() || layer.isReadonly()
                    || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                annotationLayers.add(layer);
                l = layer;
            }
            // manage chain type
            else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                    if (!feature.isEnabled()) {
                        continue;
                    }
                    if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                        annotationLayers.add(layer);
                    }

                }
            }
            // chain
        }
        if (bModel.getDefaultAnnotationLayer() != null) {
            bModel.setSelectedAnnotationLayer(bModel.getDefaultAnnotationLayer());
        }
        else if (l != null) {
            bModel.setSelectedAnnotationLayer(l);
        }
    }

    public class FeatureEditorPanelContent
        extends RefreshingView<FeatureModel>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        public FeatureEditorPanelContent(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected void populateItem(final Item<FeatureModel> item)
        {
            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);
            
            final FeatureModel fm = item.getModelObject();

            final FeatureEditor frag;
            switch (fm.feature.getMultiValueMode()) {
            case NONE: {
                switch (fm.feature.getType()) {
                case CAS.TYPE_NAME_INTEGER: {
                    frag = new NumberFeatureEditor("editor", "numberFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_FLOAT: {
                    frag = new NumberFeatureEditor("editor", "numberFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_BOOLEAN: {
                    frag = new BooleanFeatureEditor("editor", "booleanFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_STRING: {
                    frag = new TextFeatureEditor("editor", "textFeatureEditor", item, fm);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported type [" + fm.feature.getType()
                            + "] on feature [" + fm.feature.getName() + "]");
                }
                break;
            }
            case ARRAY: {
                switch (fm.feature.getLinkMode()) {
                case WITH_ROLE: {
                    // If it is none of the primitive types, it must be a link feature
                    frag = new LinkFeatureEditor("editor", "linkFeatureEditor", item, fm);
                    break;

                }
                default:
                    throw new IllegalArgumentException("Unsupported link mode ["
                            + fm.feature.getLinkMode() + "] on feature [" + fm.feature.getName()
                            + "]");
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported multi-value mode ["
                        + fm.feature.getMultiValueMode() + "] on feature [" + fm.feature.getName()
                        + "]");
            }
            // We need to enable the markup ID here because we use it during the AJAX behavior that
            // automatically saves feature editors on change/blur. Check addAnnotateActionBehavior.
            frag.setOutputMarkupId(true);
            item.add(frag);

            if (!fm.feature.getLayer().isReadonly()) {
                // whenever it is updating an annotation, it updates automatically when a component
                // for the feature lost focus - but updating is for every component edited
                // LinkFeatureEditors must be excluded because the auto-update will break the
                // ability to add slots. Adding a slot is NOT an annotation action.
              // TODO annotate every time except when position is at (0,0)
                if (bModel.getSelection().getAnnotation().isSet()
                        && !(frag instanceof LinkFeatureEditor)) {
                    if (frag.isDropOrchoice()) {
                        addAnnotateActionBehavior(frag, "change");
                    }
                    else {
                        addAnnotateActionBehavior(frag, "blur");
                    }
                }
                else if (!(frag instanceof LinkFeatureEditor)) {
                    if (frag.isDropOrchoice()) {
                        storeFeatureValue(frag, "change");
                    }
                    else {
                        storeFeatureValue(frag, "blur");
                    }
                }

                // Put focus on hidden input field if we are in forward-mode
            	if (bModel.isForwardAnnotation()) {
    				forwardAnnotationText.add(new DefaultFocusBehavior2());
    			} 
            	// Put focus on first component if we select an existing annotation or create a
            	// new one
            	else if (
    			        item.getIndex() == 0 && 
    			        SpanAnnotationResponse.COMMAND.equals(bModel.getUserAction())
		        ) { 
    			    frag.getFocusComponent().add(new DefaultFocusBehavior()); 
                }
            	// Restore/preserve focus when tabbing through the feature editors
            	else if (bModel.getUserAction() == null) {
            	    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null && frag.getFocusComponent().getMarkupId()
                            .equals(target.getLastFocusedElementId())) {
                        target.focusComponent(frag.getFocusComponent());
                    }
            	}

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(fm.feature.getUiName());
                if (fm.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(fm.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }
                
                Component labelComponent = frag.getLabelComponent();
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                        fm.feature.getDescription()));
            }
            else {
                frag.getFocusComponent().setEnabled(false);
            }
        }

        private void storeFeatureValue(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(annotationFeatureForm);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
                {
                    super.updateAjaxAttributes(aAttributes);
                    // When focus is on a feature editor and the user selects a new annotation,
                    // there is a race condition between the saving the value of the feature editor
                    // and the loading of the new annotation. Delay the feature editor save to give
                    // preference to loading the new annotation.
                    aAttributes.setThrottlingSettings(
                            new ThrottlingSettings(getMarkupId(), Duration.milliseconds(250), true));
                    aAttributes.getAjaxCallListeners().add(new AjaxCallListener()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public CharSequence getPrecondition(Component aComponent)
                        {
                            // If the panel refreshes because the user selects
                            // a new annotation, the annotation editor panel is updated for the
                            // new annotation first (before saving values) because of the delay
                            // set above. When the delay is over, we can no longer save the value
                            // because the old component is no longer there. We use the markup id
                            // of the editor fragments to check if the old component is still there
                            // (i.e. if the user has just tabbed to a new field) or if the old
                            // component is gone (i.e. the user selected/created another annotation).
                            // If the old component is no longer there, we abort the delayed save
                            // action.
                            return "return $('#"+aFrag.getMarkupId()+"').length > 0;";
                        }
                    });
                }
                
                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    try {
                        if (bModel.getConstraints() != null) {
                            // Make sure we update the feature editor panel because due to
                            // constraints the contents may have to be re-rendered
                            aTarget.add(annotationFeatureForm);
                        }
                        actionAnnotate(aTarget, bModel, false);
                    }
                    catch (BratAnnotationException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                }
            });
        }

        @Override
        protected Iterator<IModel<FeatureModel>> getItemModels()
        {
            ModelIteratorAdapter<FeatureModel> i = new ModelIteratorAdapter<FeatureModel>(
                    featureModels)
            {
                @Override
                protected IModel<FeatureModel> model(FeatureModel aObject)
                {
                    return Model.of(aObject);
                }
            };
            return i;
        }
    }

    public static abstract class FeatureEditor
        extends Fragment
    {
        private static final long serialVersionUID = -7275181609671919722L;

        protected static final String ID_PREFIX = "featureEditorHead";
        
        public FeatureEditor(String aId, String aMarkupId, Item<FeatureModel> aMarkupProvider,
                IModel<?> aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, aModel);
        }

        public Component getLabelComponent()
        {
            return get("feature");
        }

        abstract public Component getFocusComponent();

        abstract public boolean isDropOrchoice();
    }

    public static class NumberFeatureEditor<T extends Number>
        extends FeatureEditor
    {
        private static final long serialVersionUID = -2426303638953208057L;
        @SuppressWarnings("rawtypes")
        private final NumberTextField field;

        public NumberFeatureEditor(String aId, String aMarkupId, Item<FeatureModel> aItem,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aItem, new CompoundPropertyModel<FeatureModel>(aModel));

            add(new Label("feature", aModel.feature.getUiName()));

            switch (aModel.feature.getType()) {
            case CAS.TYPE_NAME_INTEGER: {
                field = new NumberTextField<Integer>("value", Integer.class);
                break;
            }
            case CAS.TYPE_NAME_FLOAT: {
                field = new NumberTextField<Float>("value", Float.class);
                add(field);
                break;
            }
            default:
                throw new IllegalArgumentException("Type [" + aModel.feature.getType()
                        + "] cannot be rendered as a numeric input field");
            }
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshs of the feature editor panel. This is required to restore the focus.
            field.setOutputMarkupId(true);
            field.setMarkupId(ID_PREFIX + aModel.feature.getId());
            
            add(field);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public NumberTextField getFocusComponent()
        {
            return field;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return false;
        }
    };

    public static class BooleanFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 5104979547245171152L;
        private final CheckBox field;

        public BooleanFeatureEditor(String aId, String aMarkupId, Item<FeatureModel> aItem,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aItem, new CompoundPropertyModel<FeatureModel>(aModel));

            add(new Label("feature", aModel.feature.getUiName()));

            field = new CheckBox("value");
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshs of the feature editor panel. This is required to restore the focus.
            field.setOutputMarkupId(true);
            field.setMarkupId(ID_PREFIX + aModel.feature.getId());
            
            add(field);
        }

        @Override
        public Component getFocusComponent()
        {
            return field;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return true;
        }
    };

    public class TextFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 7763348613632105600L;
        @SuppressWarnings("rawtypes")
        private final AbstractTextComponent field;
        private boolean isDrop;
        //For showing the status of Constraints rules kicking in.
        private RulesIndicator indicator = new RulesIndicator();
        private boolean hideUnconstraintFeature;
        /**
         * Hides feature if "Hide un-constraint feature" is enabled
         * and constraint rules are applied and feature doesn't match any constraint rule
         */
        @Override
		public boolean isVisible() {
			if (hideUnconstraintFeature) {
				//if enabled and constraints rule execution returns anything other than green
				if (indicator.isAffected() && !indicator.getStatusColor().equals("green")) {
					return false;
				}
			}
			return true;
			
		}

		public TextFeatureEditor(String aId, String aMarkupId, Item<FeatureModel> aItem,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aItem, new CompoundPropertyModel<FeatureModel>(aModel));
            //Checks whether hide un-constraint feature is enabled or not
            hideUnconstraintFeature = aModel.feature.isHideUnconstraintFeature();
            
            add(new Label("feature", aModel.feature.getUiName()));

            indicator.reset(); //reset the indicator
            if (aModel.feature.getTagset() != null) {
                List<Tag> tagset = null;
                BratAnnotatorModel model = bModel;
                // verification to check whether constraints exist for this project or NOT
                if (model.getConstraints() != null && model.getSelection().getAnnotation().isSet()) {
//                    indicator.setRulesExist(true);
                    tagset = populateTagsBasedOnRules(model, aModel);
                }
                else {
//                    indicator.setRulesExist(false);
                    // Earlier behavior,
                    tagset = annotationService.listTags(aModel.feature.getTagset());
                }
                field = new StyledComboBox<Tag>("value", tagset) {
                    private static final long serialVersionUID = -1735694425658462932L;

                    @Override
                    protected void onInitialize()
                    {
                        // Ensure proper order of the initializing JS header items: first combo box
                        // behavior (in super.onInitialize()), then tooltip.
                        Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                        options.set("content", functionForTooltip);
                        add(new TooltipBehavior("#"+field.getMarkupId()+"_listbox *[title]", options) {
                            private static final long serialVersionUID = 1854141593969780149L;

                            @Override
                            protected String $()
                            {
                                // REC: It takes a moment for the KendoDatasource to load the data and
                                // for the Combobox to render the hidden dropdown. I did not find
                                // a way to hook into this process and to get notified when the
                                // data is available in the dropdown, so trying to handle this
                                // with a slight delay hopeing that all is set up after 1 second.
                                return "try {setTimeout(function () { " + super.$() + " }, 1000); } catch (err) {}; ";
                            }
                        });
                        
                        super.onInitialize();
                    }
                };
                
                isDrop = true;
            }
            else {
                field = new TextField<String>("value");
            }
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshs of the feature editor panel. This is required to restore the focus.
            field.setOutputMarkupId(true);
            field.setMarkupId(ID_PREFIX + aModel.feature.getId());
            
            add(field);
            
            //Shows whether constraints are triggered or not
            //also shows state of constraints use.
            Component constraintsInUseIndicator = new WebMarkupContainer("textIndicator"){
                private static final long serialVersionUID = 4346767114287766710L;

                @Override
                public boolean isVisible()
                {
                    return indicator.isAffected();
                }
            }.add(new AttributeAppender("class", new Model<String>(){
                private static final long serialVersionUID = -7683195283137223296L;

                @Override
                public String getObject()
                {
                    //adds symbol to indicator
                    return indicator.getStatusSymbol();
                }
            }))
              .add(new AttributeAppender("style", new Model<String>(){
                private static final long serialVersionUID = -5255873539738210137L;

                @Override
                public String getObject()
                {
                    //adds color to indicator
                    return "; color: " + indicator.getStatusColor();
                }
            }));
            add(constraintsInUseIndicator);
        }

        /**
         * Adds and sorts tags based on Constraints rules
         */
        private List<Tag> populateTagsBasedOnRules(BratAnnotatorModel model, FeatureModel aModel)
        {
            // Add values from rules
            String restrictionFeaturePath;
            switch (aModel.feature.getLinkMode()) {
            case WITH_ROLE:
                restrictionFeaturePath = aModel.feature.getName() + "."
                        + aModel.feature.getLinkTypeRoleFeatureName();
                break;
            case NONE:
                restrictionFeaturePath = aModel.feature.getName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported link mode ["
                        + aModel.feature.getLinkMode() + "] on feature ["
                        + aModel.feature.getName() + "]");
            }

            List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());

            try {
                JCas jCas = getCas(model);

                FeatureStructure featureStructure = selectByAddr(jCas, model.getSelection()
                        .getAnnotation().getId());

                Evaluator evaluator = new ValuesGenerator();
                //Only show indicator if this feature can be affected by Constraint rules!
                indicator.setAffected(evaluator.isThisAffectedByConstraintRules(featureStructure,
                        restrictionFeaturePath, model.getConstraints()));
                
                List<PossibleValue> possibleValues;
                try {
                    possibleValues = evaluator.generatePossibleValues(
                            featureStructure, restrictionFeaturePath, model.getConstraints());
    
                    LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                            + restrictionFeaturePath + "]: " + possibleValues);
                }
                catch (Exception e) {
                    error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
                    possibleValues = new ArrayList<>();
                }

                // only adds tags which are suggested by rules and exist in tagset.
                List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset, indicator);

                // add remaining tags
                addRemainingTags(tagset, valuesFromTagset);
                return tagset;
            }
            catch (IOException | ClassNotFoundException | UIMAException e) {
                error(ExceptionUtils.getRootCauseMessage(e));
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }
            return valuesFromTagset;
        }

        @Override
        public Component getFocusComponent()
        {
            return field;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return isDrop;
        }
    };

    public class LinkFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 7469241620229001983L;

        private WebMarkupContainer content;
        //For showing the status of Constraints rules kicking in.
        private RulesIndicator indicator = new RulesIndicator();

        @SuppressWarnings("rawtypes")
        private final AbstractTextComponent newRole;
        private boolean isDrop;
        private boolean hideUnconstraintFeature;
        /**
         * Hides feature if "Hide un-constraint feature" is enabled
         * and constraint rules are applied and feature doesn't match any constraint rule
         */
        @Override
		public boolean isVisible() {
			if (hideUnconstraintFeature) {
				//if enabled and constraints rule execution returns anything other than green
				if (indicator.isAffected() && !indicator.getStatusColor().equals("green")) {
					return false;
				}
			}
			return true;
			
		}
        
        @SuppressWarnings("unchecked")
        public LinkFeatureEditor(String aId, String aMarkupId, Item<FeatureModel> aItem,
                final FeatureModel aModel)
        {
            super(aId, aMarkupId, aItem, new CompoundPropertyModel<FeatureModel>(aModel));
            //Checks whether hide un-constraint feature is enabled or not
            hideUnconstraintFeature = aModel.feature.isHideUnconstraintFeature();
            add(new Label("feature", aModel.feature.getUiName()));

            // Most of the content is inside this container such that we can refresh it independently
            // from the rest of the form
            content = new WebMarkupContainer("content");
            content.setOutputMarkupId(true);
            add(content);

            content.add(new RefreshingView<LinkWithRoleModel>("slots",
                    Model.of((List<LinkWithRoleModel>) aModel.value))
            {
                private static final long serialVersionUID = 5475284956525780698L;

                @Override
                protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
                {
                    ModelIteratorAdapter<LinkWithRoleModel> i = new ModelIteratorAdapter<LinkWithRoleModel>(
                            (List<LinkWithRoleModel>) LinkFeatureEditor.this.getModelObject().value)
                    {
                        @Override
                        protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                        {
                            return Model.of(aObject);
                        }
                    };
                    return i;
                }

                @Override
                protected void populateItem(final Item<LinkWithRoleModel> aItem)
                {
                    aItem.setModel(new CompoundPropertyModel<LinkWithRoleModel>(aItem
                            .getModelObject()));
                    Label role = new Label("role");
                    
                    aItem.add(role);
                    final Label label;
                    if (aItem.getModelObject().targetAddr == -1
                            && bModel.isArmedSlot(aModel.feature, aItem.getIndex())) {
                        label = new Label("label", "<Select to fill>");
                    }
                    else {
                        label = new Label("label");
                    }
                    label.add(new AjaxEventBehavior("click")
                    {
                        private static final long serialVersionUID = 7633309278417475424L;

                        @Override
                        protected void onEvent(AjaxRequestTarget aTarget)
                        {
                            if (bModel.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                bModel.clearArmedSlot();                                
                                aTarget.add(content);
                            }
                            else {
                                bModel.setArmedSlot(aModel.feature, aItem.getIndex());
                                // Need to re-render the whole form because a slot in another
                                // link editor might get unarmed
                                aTarget.add(annotationFeatureForm);
                            }
                        }
                    });
                    label.add(new AttributeAppender("style", new Model<String>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject()
                        {
                            BratAnnotatorModel model = bModel;
                            if (model.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                return "; background: orange";
                            }
                            else {
                                return "";
                            }
                        }
                    }));
                    aItem.add(label);
                }
            });

            if (aModel.feature.getTagset() != null) {
                List<Tag> tagset = null;
                //reset the indicator
                indicator.reset();
                if (bModel.getConstraints() != null && bModel.getSelection().getAnnotation().isSet()) {
//                    indicator.setRulesExist(true); //Constraint rules exist!
                    tagset = addTagsBasedOnRules(bModel, aModel);
                }
                else {
//                    indicator.setRulesExist(false); //No constraint rules.
                    // add tagsets only, earlier behavior
                    tagset = annotationService.listTags(aModel.feature.getTagset());
                }

                newRole = new StyledComboBox<Tag>("newRole", Model.of(""), tagset) {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        
                        // Ensure proper order of the initializing JS header items: first combo box
                        // behavior (in super.onInitialize()), then tooltip.
                        Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                        options.set("content", functionForTooltip);
                        add(new TooltipBehavior("#"+newRole.getMarkupId()+"_listbox *[title]", options) {
                            private static final long serialVersionUID = -7207021885475073279L;

                            @Override
                            protected String $()
                            {
                                // REC: It takes a moment for the KendoDatasource to load the data and
                                // for the Combobox to render the hidden dropdown. I did not find
                                // a way to hook into this process and to get notified when the
                                // data is available in the dropdown, so trying to handle this
                                // with a slight delay hopeing that all is set up after 1 second.
                                return "try {setTimeout(function () { " + super.$() + " }, 1000); } catch (err) {}; ";
                            }
                        });
                    }
                    
                    @Override
                    protected void onConfigure()
                    {
                        super.onConfigure();
                        if (bModel.isSlotArmed() && aModel.feature.equals(bModel.getArmedFeature())) {
                            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                                    .getModelObject().value;
                            setModelObject(links.get(bModel.getArmedSlot()).role);
                        }
                        else {
                            setModelObject("");
                        }
                    }
                };
                
                // Ensure that markup IDs of feature editor focus components remain constant across
                // refreshs of the feature editor panel. This is required to restore the focus.
                newRole.setOutputMarkupId(true);
                newRole.setMarkupId(ID_PREFIX + aModel.feature.getId());
                
                content.add(newRole);
                
                isDrop = true;
            }
            else {
                content.add(newRole = new TextField<String>("newRole", Model.of("")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onConfigure()
                    {
                        super.onConfigure();
                        if (bModel.isSlotArmed() && aModel.feature.equals(bModel.getArmedFeature())) {
                            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                                    .getModelObject().value;
                            setModelObject(links.get(bModel.getArmedSlot()).role);
                        }
                        else {
                            setModelObject("");
                        }
                    }
                });
            }
            
            //Shows whether constraints are triggered or not
            //also shows state of constraints use.
            Component constraintsInUseIndicator = new WebMarkupContainer("linkIndicator"){
                private static final long serialVersionUID = 4346767114287766710L;

                @Override
                public boolean isVisible()
                {
                    return indicator.isAffected();
                }
            }.add(new AttributeAppender("class", new Model<String>(){
                private static final long serialVersionUID = -7683195283137223296L;

                @Override
                public String getObject()
                {
                    //adds symbol to indicator
                    return indicator.getStatusSymbol();
                }
            }))
              .add(new AttributeAppender("style", new Model<String>(){
                private static final long serialVersionUID = -5255873539738210137L;

                @Override
                public String getObject()
                {
                    //adds color to indicator
                    return "; color: " + indicator.getStatusColor();
                }
            }));
            add(constraintsInUseIndicator);
            
            // Add a new empty slot with the specified role
            content.add(new AjaxButton("add")
            {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void onConfigure(){
                    BratAnnotatorModel model = bModel;
                    setVisible(!(model.isSlotArmed()
                            && aModel.feature.equals(model.getArmedFeature())));
//                    setEnabled(!(model.isSlotArmed()
//                            && aModel.feature.equals(model.getArmedFeature())));
                }
                
                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    if (StringUtils.isBlank((String) newRole.getModelObject())) {
                        error("Must set slot label before adding!");
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                    }
                    else {
                        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                                .getModelObject().value;
                        LinkWithRoleModel m = new LinkWithRoleModel();
                        m.role = (String) newRole.getModelObject();
                        links.add(m);
                        bModel.setArmedSlot(LinkFeatureEditor.this.getModelObject().feature,
                                links.size() - 1);
                        
                        // Need to re-render the whole form because a slot in another
                        // link editor might get unarmed
                        aTarget.add(annotationFeatureForm);
                    }
                }
            });
            
            // Allows user to update slot
            content.add(new AjaxButton("set"){

                private static final long serialVersionUID = 7923695373085126646L;

                @Override
                protected void onConfigure(){
                    BratAnnotatorModel model = bModel;
                    setVisible(model.isSlotArmed()
                            && aModel.feature.equals(model.getArmedFeature()));
//                    setEnabled(model.isSlotArmed()
//                            && aModel.feature.equals(model.getArmedFeature()));
                }
                
                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;
                    BratAnnotatorModel model = bModel;
                    
                    //Update the slot
                    LinkWithRoleModel m = new LinkWithRoleModel();
                    m = links.get(model.getArmedSlot());
                    m.role = (String) newRole.getModelObject();
//                    int index = model.getArmedSlot(); //retain index
//                    links.remove(model.getArmedSlot());
//                    model.clearArmedSlot();
//                    links.add(m);
                    links.set(model.getArmedSlot(), m); //avoid reordering
                    aTarget.add(content);
                    try {
                        actionAnnotate(aTarget, bModel, false);
                    }
                    catch(BratAnnotationException e){
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCause(e),e);
                    }
                    catch (Exception e) {
                      error(e.getMessage());
                      LOG.error(ExceptionUtils.getRootCause(e),e);
                    }
                }
            });

            // Add a new empty slot with the specified role
            content.add(new AjaxButton("del")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    BratAnnotatorModel model = bModel;
                    setVisible(model.isSlotArmed()
                            && aModel.feature.equals(model.getArmedFeature()));
//                    setEnabled(model.isSlotArmed()
//                            && aModel.feature.equals(model.getArmedFeature()));
                }

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;

                    BratAnnotatorModel model = bModel;
                    links.remove(model.getArmedSlot());
                    model.clearArmedSlot();

                    aTarget.add(content);

                    // Auto-commit if working on existing annotation
                    if (bModel.getSelection().getAnnotation().isSet()) {
                        try {
                            actionAnnotate(aTarget, bModel, false);
                        }
                        catch (BratAnnotationException e) {
                            error(ExceptionUtils.getRootCauseMessage(e));
                            LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                        }
                        catch (Exception e) {
                            error(ExceptionUtils.getRootCauseMessage(e));
                            LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                        }
                    }
                }
            });
        }

        /**
         * Adds tagset based on Constraints rules, auto-adds tags which are marked important.
         *
         * @return List containing tags which exist in tagset and also suggested by rules, followed
         *         by the remaining tags in tagset.
         */
        private List<Tag> addTagsBasedOnRules(BratAnnotatorModel model, final FeatureModel aModel)
        {
            String restrictionFeaturePath = aModel.feature.getName() + "."
                    + aModel.feature.getLinkTypeRoleFeatureName();

            List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());

            try {
                JCas jCas = getCas(model);

                FeatureStructure featureStructure = selectByAddr(jCas, model.getSelection()
                        .getAnnotation().getId());

                Evaluator evaluator = new ValuesGenerator();
                //Only show indicator if this feature can be affected by Constraint rules!
                indicator.setAffected(evaluator.isThisAffectedByConstraintRules(featureStructure,
                        restrictionFeaturePath, model.getConstraints()));
                
                List<PossibleValue> possibleValues;
                try {
                    possibleValues = evaluator.generatePossibleValues(
                            featureStructure, restrictionFeaturePath, model.getConstraints());
    
                    LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                            + restrictionFeaturePath + "]: " + possibleValues);
                }
                catch (Exception e) {
                    error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e), e);
                    possibleValues = new ArrayList<>();
                }

                // Only adds tags which are suggested by rules and exist in tagset.
                List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset, indicator);
                removeAutomaticallyAddedUnusedEntries();

                // Create entries for important tags.
                autoAddImportantTags(tagset, possibleValues);

                // Add remaining tags.
                addRemainingTags(tagset, valuesFromTagset);
                return tagset;
            }
            catch (ClassNotFoundException | UIMAException | IOException e) {
                error(ExceptionUtils.getRootCauseMessage(e));
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }

            return valuesFromTagset;
        }

        private void removeAutomaticallyAddedUnusedEntries()
        {
            // Remove unused (but auto-added) tags.
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> list = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                    .getModelObject().value;

            Iterator<LinkWithRoleModel> existingLinks = list.iterator();
            while (existingLinks.hasNext()) {
                LinkWithRoleModel link = existingLinks.next();
                if (link.autoCreated && link.targetAddr == -1) {
                    // remove it
                    existingLinks.remove();
                }
            }
        }

        private void autoAddImportantTags(List<Tag> aTagset, List<PossibleValue> possibleValues)
        {
            // Construct a quick index for tags
            Set<String> tagset = new HashSet<String>();
            for (Tag t : aTagset) {
                tagset.add(t.getName());
            }

            // Get links list and build role index
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                    .getModelObject().value;
            Set<String> roles = new HashSet<String>();
            for (LinkWithRoleModel l : links) {
                roles.add(l.role);
            }

            // Loop over values to see which of the tags are important and add them.
            for (PossibleValue value : possibleValues) {
                if (!value.isImportant() || !tagset.contains(value.getValue())) {
                    continue;
                }

                // Check if there is already a slot with the given name
                if (roles.contains(value.getValue())) {
                    continue;
                }

                // Add empty slot in UI with that name.
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = value.getValue();
                // Marking so that can be ignored later.
                m.autoCreated = true;
                links.add(m);
                // NOT arming the slot here!
            }
        }

        public void setModelObject(FeatureModel aModel)
        {
            setDefaultModelObject(aModel);
        }

        public FeatureModel getModelObject()
        {
            return (FeatureModel) getDefaultModelObject();
        }

        @Override
        public Component getFocusComponent()
        {
            return newRole;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return isDrop;
        }
    };

    public void clearFeatures(AjaxRequestTarget aTarget)
    {
        featureModels = new ArrayList<>();
        if (aTarget != null) {
            aTarget.add(annotationFeatureForm);
        }
    }
    
    private void populateFeatures(AnnotationLayer aLayer, FeatureStructure aFS, 
            Map<AnnotationFeature, Serializable> aRemembered)
    {
        clearFeatures(null);
        
        // Populate from feature structure
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aLayer)) {
            if (!feature.isEnabled()) {
                continue;
            }
            
            Serializable value = null;
            if (aFS != null) {
                value = (Serializable) BratAjaxCasUtil.getFeature(aFS, feature);
            }
            else if (aRemembered != null) {
                value = aRemembered.get(feature);
            }
            
            if (WebAnnoConst.CHAIN_TYPE.equals(feature.getLayer().getType())) {
                if (bModel.getSelection().isRelationAnno()) {
                    if (feature.getLayer().isLinkedListBehavior()
                            && WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(feature
                                    .getName())) {
                        featureModels.add(new FeatureModel(feature, value));
                    }
                }
                else {
                    if (WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(feature.getName())) {
                        featureModels.add(new FeatureModel(feature, value));
                    }
                }

            }
            else {
                featureModels.add(new FeatureModel(feature, value));
            }
        }
    }
    
    public void addRemainingTags(List<Tag> tagset, List<Tag> valuesFromTagset)
    {
        // adding the remaining part of tagset.
        for (Tag remainingTag : valuesFromTagset) {
            if (!tagset.contains(remainingTag)) {
                tagset.add(remainingTag);
            }
        }

    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<Tag> compareSortAndAdd(List<PossibleValue> possibleValues,
            List<Tag> valuesFromTagset, RulesIndicator rulesIndicator)
    {
        //if no possible values, means didn't satisfy conditions
        if(possibleValues.isEmpty())
        {
            rulesIndicator.didntMatchAnyRule();
        }
        List<Tag> returnList = new ArrayList<Tag>();
        // Sorting based on important flag
        // possibleValues.sort(null);
        // Comparing to check which values suggested by rules exists in existing
        // tagset and adding them first in list.
        for (PossibleValue value : possibleValues) {
            for (Tag tag : valuesFromTagset) {
                if (value.getValue().equalsIgnoreCase(tag.getName())) {
                    //Matching values found in tagset and shown in dropdown
                    rulesIndicator.rulesApplied();
                    // HACK BEGIN
                    tag.setReordered(true);
                    // HACK END
                    //Avoid duplicate entries
                    if(!returnList.contains(tag)){ 
                        returnList.add(tag); 
                    }
                }
            }
        }
        //If no matching tags found
        if(returnList.isEmpty()){
            rulesIndicator.didntMatchAnyTag();
        }
        return returnList;
    }

    public class LayerSelector
        extends DropDownChoice<AnnotationLayer>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        public LayerSelector(String aId, List<? extends AnnotationLayer> aChoices)
        {
            super(aId, aChoices);
            setOutputMarkupId(true);
            setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>("uiName"));
            add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    // If "remember layer" is set, the we really just update the selected layer...
                    // we do not touch the selected annotation not the annotation detail panel
                    if (bModel.getPreferences().isRememberLayer()) {
                        bModel.setSelectedAnnotationLayer(getModelObject());
                    }
                    // If "remember layer" is not set, then changing the layer means that we want
                    // to change the type of the currently selected annotation
                    else if (
                            !bModel.getSelectedAnnotationLayer().equals(getModelObject()) && 
                            bModel.getSelection().getAnnotation().isSet()) 
                    {
                        if (bModel.getSelection().isRelationAnno()) {
                            try {
                                actionClear(aTarget, bModel);
                            }
                            catch (UIMAException | ClassNotFoundException | IOException
                                    | BratAnnotationException e) {
                                error(e.getMessage());
                            }
                        } 
                        else {
                            deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel(
                                    deleteModal.getContentId(), bModel, deleteModal,
                                    AnnotationDetailEditorPanel.this, getModelObject(), true));

                            deleteModal
                                    .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 4364820331676014559L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    target.add(annotationFeatureForm);

                                }
                            });
                            deleteModal.show(aTarget);
                        }
                    }
                    // If no annotation is selected, then prime the annotation detail panel for the
                    // new type
                    else {
                        bModel.setSelectedAnnotationLayer(getModelObject());
                        selectedAnnotationLayer.setDefaultModelObject(getModelObject().getUiName());
                        aTarget.add(selectedAnnotationLayer);
                        clearFeatures(aTarget);
                    }
                }
            });
        }
    }
    
    private FeatureModel getFeatureModel(AnnotationFeature aFeature)
    {
        for (FeatureModel f : featureModels) {
            if (f.feature.getId() == aFeature.getId()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Represents a link with a role in the UI.
     */
    public static class LinkWithRoleModel
        implements Serializable
    {
        private static final long serialVersionUID = 2027345278696308900L;

        public static final String CLICK_HINT = "<Click to activate>";

        public String role;
        public String label = CLICK_HINT;
        public int targetAddr = -1;
        public boolean autoCreated;

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((label == null) ? 0 : label.hashCode());
            result = prime * result + ((role == null) ? 0 : role.hashCode());
            result = prime * result + targetAddr;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LinkWithRoleModel other = (LinkWithRoleModel) obj;
            if (label == null) {
                if (other.label != null) {
                    return false;
                }
            }
            else if (!label.equals(other.label)) {
                return false;
            }
            if (role == null) {
                if (other.role != null) {
                    return false;
                }
            }
            else if (!role.equals(other.role)) {
                return false;
            }
            if (targetAddr != other.targetAddr) {
                return false;
            }
            return true;
        }

    }

	private void updateForwardAnnotation(BratAnnotatorModel aBModel) {
		if (aBModel.getSelectedAnnotationLayer() != null
				&& !aBModel.getSelectedAnnotationLayer().isLockToTokenOffset()) {
			aBModel.setForwardAnnotation(false);// no forwarding for
												// sub-/multitoken annotation
		} else {
			aBModel.setForwardAnnotation(aBModel.isForwardAnnotation());
		}
	}

    public static class FeatureModel
        implements Serializable
    {
        private static final long serialVersionUID = 3512979848975446735L;
        public final AnnotationFeature feature;
        public Serializable value;

        public FeatureModel(AnnotationFeature aFeature, Serializable aValue)
        {
            feature = aFeature;
            value = aValue;

            // Avoid having null here because otherwise we have to handle null in zillion places!
            if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
                value = new ArrayList<>();
            }
        }
    }
    
	private Map<String, String> getBindTags() {

		AnnotationFeature f = annotationService.listAnnotationFeature(bModel.getSelectedAnnotationLayer()).get(0);
		TagSet tagSet = f.getTagset();
		Map<Character, String> tagNames = new LinkedHashMap<>();
		Map<String, String> bindTag2Key = new LinkedHashMap<>();
		for (Tag tag : annotationService.listTags(tagSet)) {
			if (tagNames.containsKey(tag.getName().toLowerCase().charAt(0))) {
				String oldBinding = tagNames.get(tag.getName().toLowerCase().charAt(0));
				String newBinding = oldBinding + tag.getName().toLowerCase().charAt(0);
				tagNames.put(tag.getName().toLowerCase().charAt(0), newBinding);
				bindTag2Key.put(newBinding, tag.getName());
			} else {
				tagNames.put(tag.getName().toLowerCase().charAt(0), tag.getName().toLowerCase().substring(0, 1));
				bindTag2Key.put(tag.getName().toLowerCase().substring(0, 1), tag.getName());
			}
		}
		return bindTag2Key;

	}
	
	private String getKeyBindValue(String aKey, Map<String, String> aBindTags){
		// check if all the key pressed are the same character
		// if not, just check a Tag for the last char pressed
		if(aKey.isEmpty()){
			return aBindTags.get(aBindTags.keySet().iterator().next());
		}
		char prevC = aKey.charAt(0);
		for(char ch:aKey.toCharArray()){
			if(ch!=prevC){
				break;
			}
		}
		
		if (aBindTags.get(aKey)!=null){
			return aBindTags.get(aKey);
		}
		// re-cycle suggestions
		if(aBindTags.containsKey(aKey.substring(0,1))){
			selectedTag = aKey.substring(0,1);
			return aBindTags.get(aKey.substring(0,1));
		}
		// set it to the first in the tag list , when arbitrary key is pressed
		return aBindTags.get(aBindTags.keySet().iterator().next());
	}

    public void reset(AjaxRequestTarget aTarget)
    {
        bModel.getSelection().clear();
        bModel.getSelection().setBegin(0);
        bModel.getSelection().setEnd(0);
        clearFeatures(aTarget);
    }

    public void refresh(AjaxRequestTarget aTarget) 
        throws BratAnnotationException
    {
        try {
            if (!bModel.getSelection().isRelationAnno()) {
                updateLayersDropdown(bModel);
            }
            
            JCas aJCas = getCas(bModel);
            
            if (bModel.getSelection().isRelationAnno()) {
                arcSelected(aTarget, aJCas);
            }
            else { 
                spanSelected(aTarget, aJCas);
            }

            updateRememberLayer();
            
            aTarget.add(annotationFeatureForm);
        }
        catch (BratAnnotationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BratAnnotationException(e);
        }
    }

    private void updateRememberLayer()
    {
		if (bModel.getPreferences().isRememberLayer()) {
			if (bModel.getDefaultAnnotationLayer() == null) {
				bModel.setDefaultAnnotationLayer(bModel.getSelectedAnnotationLayer());
			}
		} 
		else if (!bModel.getSelection().isRelationAnno()) {
			bModel.setDefaultAnnotationLayer(bModel.getSelectedAnnotationLayer());
		}
		
		// if no layer is selected in Settings
		if (bModel.getSelectedAnnotationLayer() != null) {
			selectedAnnotationLayer.setDefaultModelObject(
			        bModel.getSelectedAnnotationLayer().getUiName());
		} 
	}

    /**
     * remove this model, if new annotation is to be created
     */
    public void clearArmedSlotModel()
    {
        for (FeatureModel fm : featureModels) {
            if (StringUtils.isNotBlank(fm.feature.getLinkTypeName())) {
                fm.value = new ArrayList<>();
            }
        }
    }

    private  Set<AnnotationFS> getAttachedRels(JCas aJCas, AnnotationFS aFs, AnnotationLayer aLayer) throws UIMAException, ClassNotFoundException, IOException{
        
        Set<AnnotationFS> toBeDeleted = new HashSet<AnnotationFS>();
        for (AnnotationLayer relationLayer : annotationService
                .listAttachedRelationLayers(aLayer)) {
            ArcAdapter relationAdapter = (ArcAdapter) getAdapter(annotationService,
                    relationLayer);
            Type relationType = CasUtil.getType(aJCas.getCas(), relationLayer.getName());
            Feature sourceFeature = relationType.getFeatureByBaseName(relationAdapter
                    .getSourceFeatureName());
            Feature targetFeature = relationType.getFeatureByBaseName(relationAdapter
                    .getTargetFeatureName());

            // This code is already prepared for the day that relations can go between
            // different layers and may have different attach features for the source and
            // target layers.
            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeature.getRange().getFeatureByBaseName(
                        relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeature.getRange().getFeatureByBaseName(
                        relationAdapter.getAttachFeatureName());
            }
            
            for (AnnotationFS relationFS : CasUtil.select(aJCas.getCas(), relationType)) {
                // Here we get the annotations that the relation is pointing to in the UI
                FeatureStructure sourceFS;
                if (relationSourceAttachFeature != null) {
                    sourceFS = relationFS.getFeatureValue(sourceFeature).getFeatureValue(
                            relationSourceAttachFeature);
                }
                else {
                    sourceFS = relationFS.getFeatureValue(sourceFeature);
                }

                FeatureStructure targetFS;
                if (relationTargetAttachFeature != null) {
                    targetFS = relationFS.getFeatureValue(targetFeature).getFeatureValue(
                            relationTargetAttachFeature);
                }
                else {
                    targetFS = relationFS.getFeatureValue(targetFeature);
                }

                if (isSame(sourceFS, aFs) || isSame(targetFS, aFs)) {
                    toBeDeleted.add(relationFS);
                    LOG.debug("Deleted relation [" + getAddr(relationFS) + "] from layer ["
                            + relationLayer.getName() + "]");
                }
            }
        }
        return toBeDeleted;
        
    }
    
    
    public AnnotationFeatureForm getAnnotationFeatureForm()
    {
        return annotationFeatureForm;
    }

    public Label getSelectedAnnotationLayer()
    {
        return selectedAnnotationLayer;
    }

    private boolean isFeatureModelChanged(AnnotationLayer aLayer){

            for(FeatureModel fM: featureModels){
                if(!annotationService.listAnnotationFeature(aLayer).contains(fM.feature)){
                    return true;
            }
        }
            return false;
        
    }
    
	private boolean isForwardable() {
		if (bModel.getSelectedAnnotationLayer() == null) {
            return false;
        }
		if (bModel.getSelectedAnnotationLayer().getId() <= 0) {
            return false;
        }

		if (!bModel.getSelectedAnnotationLayer().getType().equals(WebAnnoConst.SPAN_TYPE)) {
            return false;
        }
		if (!bModel.getSelectedAnnotationLayer().isLockToTokenOffset()) {
			return false;
		}
		// no forward annotation for multifeature layers.
		if(annotationService.listAnnotationFeature(bModel.getSelectedAnnotationLayer()).size()>1){
			return false;
		}
        // if there are no features at all, no forward annotation
        if(annotationService.listAnnotationFeature(bModel.getSelectedAnnotationLayer()).isEmpty()){
            return false;
        }
        // we allow forward annotation only for a feature with a tagset
		if(annotationService.listAnnotationFeature(bModel.getSelectedAnnotationLayer()).get(0).getTagset()==null){
			return false;
		}
		TagSet tagSet = annotationService.listAnnotationFeature(bModel.getSelectedAnnotationLayer()).get(0).getTagset();
		
		// there should be at least one tag in the tagset
		if(annotationService.listTags(tagSet).size()==0){
			return false;
		}
		return true;
	}
    private static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }

    class StyledComboBox<T>
        extends ComboBox<T>
    {
        private static final long serialVersionUID = 1L;
        
        public StyledComboBox(String id, IModel<String> model, List<T> choices)
        {
            super(id, model, choices);            
        }

        public StyledComboBox(String string, List<T> choices)
        {
            super(string, choices);
        }

        @Override
        protected void onInitialize()
        {
            super.onInitialize();
            
            add(new Behavior() {
                private static final long serialVersionUID = -5674186692106167407L;
                
                @Override
                public void renderHead(Component aComponent, IHeaderResponse aResponse)
                {
                    super.renderHead(aComponent, aResponse);
                    
                    // Force-remove KendoDataSource header item if there already is one. This allows
                    // Wicket to re-declare the datasource for the callback URL of the new instance
                    // of this feature editor.
                    // This causes all the choices to be transferred again, but at least tags added
                    // to open tagsets appear immediately in the dropdown list and constraints
                    // apply (hopefully).
                    // Note: this must be done here instead of before the call to super such that
                    // first the old datasource declarations are removed and then the new one is
                    // added and remains in the HTML. Here we rely on the fact that the feature
                    // editors have a fixed markup ID (which we also rely on for restoring focus).
                    aResponse.render(new PriorityHeaderItem(JavaScriptHeaderItem.forScript(
                            "$('head script[id=kendo-datasource_" +
                            StyledComboBox.this.getMarkupId() + "]').remove();", 
                            null)));
                }
            });
        }
        
        @Override
        protected IJQueryTemplate newTemplate()
        {
            return new IJQueryTemplate()
            {
                private static final long serialVersionUID = 1L;
                /**
                 * Marks the reordered entries in bold.
                 * Same as text feature editor.
                 */
                @Override
                public String getText()
                {
                    // Some docs on how the templates work in Kendo, in case we need
                    // more fancy dropdowns
                    // http://docs.telerik.com/kendo-ui/framework/templates/overview
                    StringBuilder sb = new StringBuilder();
                    sb.append("# if (data.reordered == 'true') { #");
                    sb.append("<div title=\"#: data.description #\"><b>#: data.name #</b></div>\n");
                    sb.append("# } else { #");
                    sb.append("<div title=\"#: data.description #\">#: data.name #</div>\n");
                    sb.append("# } #");
                    return sb.toString();
                }

                @Override
                public List<String> getTextProperties()
                {
                    return Arrays.asList("name", "description", "reordered");
                }
            };
        }
    }
}
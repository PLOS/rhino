#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
PLOS Currents API Module.

This module exports one class 'Currents'

"""
from __future__ import print_function
from __future__ import with_statement
from __future__ import unicode_literals
from lxml import etree

import os, sys, traceback, json, re, requests, md5

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

CURRENTS_ARTICLES = {
                       'disasters' : [ 'non-communicable-diseases-in-emergencies-a-call-to-action',
                                       'aftershocks-following-the-9-april-2013-bushehr-earthquake-iran',
                                       'a-prospective-study-of-the-outcome-of-patients-with-limb-trauma-'
                                         'following-the-haitian-earthquake-in-2010-at-one-and-two-year-the'
                                         '-sutra2-study-2',
                                       'a-prospective-study-of-the-outcome-of-patients-with-limb-trauma-'
                                         'following-the-haitian-earthquake-in-2010-at-one-and-two-year-'
                                         'the-sutra2-study-2',
                                       'twitter-as-a-sentinel-in-emergency-situations-lessons-from-the-'
                                         'boston-marathon-explosions',
                                       'impacts-of-natural-hazards-on-primary-health-care-facilities-of'
                                         '-iran-a-10-year-retrospective-survey',
                                       'which-anthropometric-indicators-identify-a-pregnant-woman-as-'
                                         'acutely-malnourished-and-predict-adverse-birth-outcomes-in-'
                                         'the-humanitarian-context',
                                       'social-factors-as-modifiers-of-hurricane-irene-evacuation-'
                                         'behavior-in-beaufort-county-nc',
                                       'dis-13-0001-health-effects-of-drought-a-systematic-review-'
                                         'of-the-evidence',
                                       'the-great-east-japan-earthquake-disaster-a-compilation-of-'
                                         'published-literature-on-health-needs-and-relief-activities'
                                         '-march-2011-september-2012',
                                       'the-great-east-japan-earthquake-disaster-a-compilation-of-'
                                         'published-literature-on-health-needs-and-relief-activities'
                                         '-march-2011-september-2012',
                                       'lessons-from-a-flash-flood-in-tehran-subway-iran',
                                       'irans-bushehr-earthquake-at-a-glance',
                                       'vulnerabilities-of-local-healthcare-providers-in-complex-'
                                         'emergencies-findings-from-the-manipur-micro-level-insurgency'
                                         '-database-2008-2009',
                                       'state-of-virtual-reality-vr-based-disaster-preparedness-and-'
                                         'response-training',
                                       'interventions-to-mitigate-emergency-department-and-hospital-'
                                         'crowding-during-an-infectious-respiratory-disease-outbreak-'
                                         'results-from-an-expert-panel',
                                       'the-human-impacts-of-tsunamis-a-historical-review-of-events-'
                                         '1900-2009-and-systematic-literature-review',
                                       'the-human-impact-of-volcanoes-a-historical-review-of-events-'
                                             '1900-2009-and-systematic-literature-review',
                                           'the-human-impact-of-floods-a-historical-review-of-events-'
                                             '1980-2009-and-systematic-literature-review',
                                           'the-human-impact-of-earthquakes-from-1980-2009-a-historical'
                                             '-review-of-events-1980-2009-and-systematic-literature-review',
                                           'the-human-impact-of-tropical-cyclones-a-historical-review-of'
                                             '-events-1980-2009-and-systematic-literature-review',
                                           'dis-13-0009-a-summary-case-report-on-the-health-impacts-and-'
                                             'response-to-the-pakistan-floods-of-2010',
                                           'a-framework-and-methodology-for-navigating-disaster-and-'
                                             'global-health-in-crisis-literature',
                                           'housing-reconstruction-in-disaster-recovery-a-study-of-'
                                             'fishing-communities-post-tsunami-in-chennai-india',
                                           'spinal-injuries-in-the-2012-twin-earthquakes-northwest-iran',
                                           'dis-12-0017-the-use-of-systematic-reviews-and-other-research'
                                             '-evidence-in-disasters-and-related-areas-preliminary-report'
                                             '-of-a-needs-assessment-survey',
                                           'dis-12-0009-mortality-in-the-laquila-central-italy-earthquake'
                                             '-of-6-april-2009',
                                           'public-health-surveillance-after-the-2010-haiti-earthquake-'
                                             'the-experience-of-medecins-sans-frontieres',
                                           'dis-12-0012-the-role-of-veterans-affairs-in-emergency-management'
                                             '-a-systematic-literature-review',
                                           'lessons-from-the-recent-twin-earthquakes-in-iran',
                                           'health-impacts-of-wildfires',
                                           'dis-12-0013-secondary-stressors-and-extreme-events-and-disasters'
                                             '-a-systematic-review-of-primary-research-from-2010-2011',
                                           'dis-12-0006-repeat-triage-in-disaster-relief-questions-from-haiti',
                                           'dis-12-0007-assessing-the-impact-of-workshops-promoting-concepts-'
                                             'of-psychosocial-support-for-emergency-events',
                                           'dis-12-0003-the-haitian-health-cluster-experience-a-comparative-'
                                             'evaluation-of-the-professional-communication-response-to-the-'
                                             '2010-earthquake-and-the-subsequent-cholera-outbreak',
                                           'developing-a-health-system-approach-to-disaster-management-a-'
                                             'qualitative-analysis-of-the-core-literature-to-complement-the'
                                             '-who-toolkit-for-assessing-health-system-capacity-for-crisis-management',
                                           'a-health-system-approach-to-all-hazards-disaster-management-a-'
                                             'systematic-review',
                                           'impact-of-the-2010-pakistan-floods-on-rural-and-urban-populations'
                                             '-at-six-months',
                                           'disaster-metrics-a-proposed-quantitative-assessment-tool-in-'
                                             'complex-humanitarian-emergencies-the-public-health-impact-severity'
                                             '-scale-phiss',
                                           'knowledge-attitude-and-practice-of-tehrans-inhabitants-for-an-'
                                             'earthquake-and-related-determinants',
                                           'weather-and-environmental-hazards-at-mass-gatherings',
                                           'monitoring-the-mental-well-being-of-caregivers-during-the-haiti-earthquake',
                                           'review-of-the-osha-niosh-response-to-the-deepwater-horizon-oil-spill'
                                             '-protecting-the-health-and-safety-of-cleanup-workers',
                                           'local-public-health-system-response-to-the-tsunami-threat-in-coastal-'
                                             'california-following-the-tohoku-earthquake',
                                           'how-to-use-near-real-time-health-indicators-to-support-decision-'
                                             'making-during-a-heat-wave-the-example-of-the-french-heat-wave-warning-system',
                                           '2012-2025-roadmap-of-i-r-irans-disaster-health-management',
                                           'the-good-the-bad-and-the-ugly-disaster-risk-reduction-drr-versus-'
                                             'disaster-risk-creation-drc',
                                           'the-effects-of-flooding-on-mental-health-outcomes-and-'
                                             'recommendations-from-a-review-of-the-literature',
                                           'timing-and-type-of-disaster-severity-data-available-on-internet-'
                                             'following-the-2010-haiti-earthquake',
                                           'the-great-east-japan-earthquake-experiences-and-suggestions-for-'
                                             'survivors-with-diabetes-perspective-2',
                                           'utstein-style-template-for-uniform-data-reporting-of-acute-'
                                             'medical-response-in-disasters',
                                           'cholera-ante-portas-the-re-emergence-of-cholera-in-kinshasa'
                                             '-after-a-ten-year-hiatus',
                                           'quantification-of-the-heat-wave-effect-on-mortality-in-nine-'
                                             'french-cities-during-summer-2006-2',
                                           'the-buncefield-oil-depot-fire-of-2005-3wkcplftb6ss-3',
                                           'disasters-at-mass-gatherings-lessons-3wkcplftb6ss-5',
                                           'the-dadaab-camps-mitigating-the-effects-2uh9ieg4z57f8-6',
                                           'aspirations-and-compromises-changes-in-2ktejo3jcihdk-1',
                                           'the-role-of-collective-action-in-2z8p4t1bp5hj5-2',
                                           'evidence-for-disaster-risk-reduction-9z2b6qmqlx3z-3',
                                           'dealing-with-disaster-databases-what-2q9epyi1nr2rp-2',
                                           'valuing-lives-allocating-scarce-medical-2c8q7lnatgpgj-2',
                                         ],
                           'genomictests' : [
                                           'genetic-testing-strategies-in-newly-diagnosed-endometrial'
                                             '-cancer-patients-aimed-at-reducing-morbidity-or-mortality'
                                             '-from-lynch-syndrome-in-the-index-case-or-her-relatives',
                                           'use-of-the-corus-cad-gene-expression-test-for-assessment-'
                                             'of-obstructive-coronary-artery-disease-likelihood-in-'
                                             'symptomatic-non-diabetic-patients',
                                           'scn1a-genetic-test-for-dravet-syndrome-severe-myoclonic'
                                             '-epilepsy-of-infancy-and-its-clinical-subtypes',
                                           'the-decisiondx-um-gene-expression-profile-test-provides'
                                             '-risk-stratification-and-individualized-patient-care-'
                                             'in-uveal-melanoma-2',
                                           'use-of-the-afirma-gene-expression-classifier-for-'
                                             'preoperative-identification-of-benign-thyroid-nodules'
                                             '-with-indeterminate-fine-needle-aspiration-cytopathology',
                                           'genetic-testing-for-long-qt-syndrome-and-the-category-'
                                             'of-cardiac-ion-channelopathies',
                                           'comprehensive-carrier-screening-and-molecular-diagnostic-'
                                             'testing-for-recessive-childhood-diseases',
                                           'a-20-gene-model-for-predicting-nodal-qjiio56ycgdt-4',
                                           'use-of-oncotype-dx-in-women-with-node-positive-breast-cancer',
                                           'use-of-epidermal-growth-factor-receptor-mutation-analysis-'
                                             'in-patients-with-advanced-non-small-cell-lung-cancer-to-'
                                             'determine-erlotinib-use-as-first-line-therapy',
                                           'genetic-testing-for-lynch-syndrome-in-individuals-newly'
                                             '-diagnosed-with-colorectal-cancer-to-reduce-morbidity'
                                             '-and-mortality-from-colorectal-cancer-in-their-relatives',
                                           'cascade-screening-for-familial-70fnx9tmvdav-13',
                                           'thiopurine-methyltransferase-tpmt-xv5k9xg3o4yu-8',
                                           'fecal-dna-testing-for-colorectal-cancer-od1hzthyodr3-1',
                                           'genetic-risk-profiling-for-prediction-20113liwenx5c-9',
                                           'interleukin-28b-genotype-testing-to-xv5k9xg3o4yu-3',
                                           'bcr-abl-mutation-testing-to-predict-response-to-tyrosine'
                                             '-kinase-inhibitors-in-patients-with-chronic-myeloid-leukemia',
                                           'hla-b-5701-testing-to-predict-abacavir-2twojvq0wfutb-1',
                                           'ercc1-expression-analysis-to-guide-1i0b298tuv2uk-4',
                                           'kif6-p-trp719arg-testing-to-assess-risk-1i0b298tuv2uk-3',
                                           'braf-p-val600glu-v600e-testing-for-assessment-of-'
                                             'treatment-options-in-metastatic-colorectal-cancer',
                                           'decisiondx-gbm-gene-expression-assay-for-prognostic-'
                                             'testing-in-glioblastoma-multiform',
                                           'cyp2d6-testing-to-predict-response-to-3qgx0fg7r52v-1',
                                           'genetic-testing-for-cyp450-70fnx9tmvdav-1',
                                           'testing-of-vkorc1-and-cyp2c9-alleles-to-x2fzi0wvbcnu-1',
                                           'kras-mutational-analysis-for-colorectal-27yi6810q97hp-1',
                                           'oncotype-dx-tumor-gene-expression-profiling-in-stage-ii-colon-cancer',
                                           'tumor-gene-expression-profiling-in-women-with-breast-cancer',
                                           'plos-currents-evidence-on-genomic-tests-at-the-crossroads-of-translation',
                                         ],
                           'hd' :        [
                                           'increased-body-weight-of-the-bac-hd-transgenic-mouse-model-'
                                             'of-huntingtons-disease-accounts-for-some-but-not-all-of-'
                                             'the-observed-hd-like-motor-deficits',
                                           'high-throughput-automated-phenotyping-of-two-genetic-'
                                             'mouse-models-of-huntingtons-disease',
                                           'is-a-motor-criterion-essential-for-the-diagnosis-of-'
                                             'clinical-huntington-disease',
                                           'dysfunctional-mitochondrial-respiration-in-the-striatum-'
                                             'of-the-huntingtons-disease-transgenic-r62-mouse-model',
                                           'effects-of-the-pimelic-diphenylamide-histone-deacetylase'
                                             '-inhibitor-hdaci-4b-on-the-r62-and-n171-82q-mouse-models'
                                             '-of-huntingtons-disease', 
                                           'hd-12-0003-pitfalls-in-the-detection-of-cholesterol-in-'
                                             'huntingtons-disease-models',
                                           'the-prevalence-of-juvenile-huntingtons-disease-a-review'
                                             '-of-the-literature-and-meta-analysis',
                                           'caspase-6-does-not-contribute-to-the-proteolysis-of-'
                                             'mutant-huntingtin-in-the-hdhq150-knock-in-mouse-model'
                                             '-of-huntingtons-disease',
                                           'intrastriatal-transplantation-of-neurotrophic-factor-'
                                             'secreting-human-mesenchymal-stem-cells-improves-motor'
                                             '-function-and-extends-survival-in-r62-transgenic-mouse'
                                             '-model-for-huntingtons-disease',
                                           'protection-by-glia-conditioned-medium-in-a-cell-model-'
                                             'of-huntington-disease',
                                           'a-mixed-fixed-ratioprogressive-ratio-procedure-reveals'
                                             '-an-apathy-phenotype-in-the-bac-hd-and-the-z_q175-ki'
                                             '-mouse-models-of-huntingtons-disease',
                                           'pharmacokinetics-of-memantine-in-rats-and-mice',
                                           'effect-of-the-rd1-mutation-on-motor-performance-in'
                                             '-r62-and-wild-type-mice',
                                           'huntingtons-like-conditions-in-china-a-review-of'
                                             '-published-chinese-cases-2',
                                           'current-pharmacological-management-in-juvenile-'
                                             'huntingtons-disease-2',
                                           'stability-effects-on-results-of-diffusion-tensor-'
                                             'imaging-analysis-by-reduction-of-the-number-of-'
                                             'gradient-directions-due-to-motion-artifacts-an-'
                                             'application-to-presymptomatic-huntingtons-disea',
                                           'aspiration-pneumonia-and-death-in-huntingtons-disease-2',
                                           'seven-year-clinical-follow-up-of-premanifest-carriers-'
                                             'of-huntingtons-disease-2',
                                           'no-evidence-of-impaired-gastric-emptying-in-early-'
                                             'huntingtons-disease',
                                           'use-of-tetrabenazine-in-huntington-disease-patients'
                                             '-on-antidepressants-or-with-advanced-disease-results'
                                             '-from-the-tetra-hd-study',
                                           'hd-mouse-models-reveal-clear-deficits-in-learning-to-'
                                             'perform-a-simple-instrumental-response',
                                           'effect-of-enhanced-voluntary-physical-exercise-on-'
                                             'brain-levels-of-monoamines-in-huntington-disease-mice',
                                           'longitudinal-change-in-gait-and-motor-23cuk2jhasmoq-3',
                                           'baroreceptor-reflex-dysfunction-in-the-bachd-mouse-model'
                                             '-of-huntingtons-disease',
                                           'assessment-of-day-to-day-functioning-in-prodromal-and-'
                                             'early-huntington-disease-3',
                                           'an-international-survey-based-algorithm-for-the-'
                                             'pharmacologic-treatment-of-irritability-in-huntingtons'
                                             '-disease',
                                           'an-international-survey-based-algorithm-for-the-'
                                             'pharmacologic-treatment-of-chorea-in-huntingtons-disease',
                                           'an-international-survey-based-algorithm-p284k2gmahk5-9',
                                           'modifiers-of-mutant-huntingtin-2guskb0m0ddva-1',
                                           'self-reports-of-day-to-day-function-in-a-small-'
                                             'cohort-of-people-with-prodromal-and-early-hd',
                                           'music-perception-and-movement-deterioration-in-huntingtons-disease',
                                           'assessment-of-cognitive-symptoms-in-prodromal-'
                                             'and-early-huntington-disease',
                                           'age-at-onset-in-huntington-disease',
                                           'nmda-receptor-gene-variations-as-modifiers-in-'
                                             'huntington-disease-a-replication-study',
                                           'assessment-of-motor-symptoms-and-functional-'
                                             'impact-in-prodromal-and-early-huntington-disease',
                                           'assessing-behavioural-manifestations-prior-to-clinical-'
                                             'diagnosis-of-huntington-disease-anger-and-irritability'
                                             '-and-obsessions-and-compulsions-2',
                                           'assessment-of-depression-anxiety-and-apathy-in-prodromal'
                                             '-and-early-huntington-disease-2',
                                           'stability-of-white-matter-changes-related-to-'
                                             'huntingtons-disease-in-the-presence-of-imaging-noise-'
                                             'a-dti-study',
                                           'association-between-age-and-striatal-volume-stratified-'
                                             'by-cag-repeat-length-in-prodromal-huntington-disease',
                                           'abnormal-peripheral-chemokine-profile-in-huntingtons-disease',
                                           'advocacy-recruiting-for-huntington-s-p284k2gmahk5-2',
                                           'body-composition-in-premanifest-huntingtons-disease-'
                                             'reveals-lower-bone-density-compared-to-controls',
                                           'huntingtin-localisation-studies-a-2co1l1tasmbx7-3',
                                           'visualization-of-cell-to-cell-2sdo8o1u01fbj-1',
                                           'circadian-abnormalities-in-motor-activity-in-'
                                             'a-bac-transgenic-mouse-model-of-huntingtons-disease',
                                           'optimization-of-an-htrf-assay-for-the-3p3s60imu228g-2',
                                           'utilisation-of-healthcare-and-associated-services-in-'
                                             'huntingtons-disease-a-data-mining-study-2',
                                           'exercise-is-not-beneficial-and-may-3jzbm8k1knz4c-2',
                                           'characterization-of-human-huntingtons-disease-cell'
                                             '-model-from-induced-pluripotent-stem-cells-2',
                                           'qeeg-measures-in-huntingtons-disease-a-pilot-study',
                                           'serum-levels-of-a-subset-of-cytokines-show-high-'
                                             'interindividual-variability-and-are-not-altered'
                                             '-in-rats-transgenic-for-huntington´s-disease',
                                           'identifying-sleep-disturbances-in-huntingtons-'
                                             'disease-using-a-simple-disease-focused-questionnaire',
                                           'drosophila-models-of-huntington-s-2as2tsd09zfs8-2',
                                           'observing-huntingtons-disease-the-european-'
                                             'huntingtons-disease-networks-registry-3',
                                           'transplantation-of-patient-derived-adipose'
                                             '-stem-cells-in-yac128-huntingtons-disease'
                                             '-transgenic-mice',
                                           'rescuing-the-corticostriatal-synaptic-'
                                             'disconnection-in-the-r62-mouse-model-of-huntingtons'
                                             '-disease-exercise-adenosine-receptors-and-ampakines',
                                           'evaluation-of-histone-deacetylases-as-drug-targets-in'
                                             '-huntingtons-disease-models-2',
                                           'cloned-mri-t2-hypointensities-in-basal-ganglia-'
                                             'of-premanifest-huntingtons-disease',
                                           'cognitive-follow-up-of-a-small-cohort-of-'
                                             'huntingtons-disease-patients-over-a-5-'
                                             'year-period'
                                        ],
                            'md'     :  [
                                           'generation-of-embryonic-stem-cells-and-'
                                             'mice-for-duchenne-research', 
                                           'stem-cell-antigen-1-in-skeletal-muscle-function',
                                           'the-6-minute-walk-test-and-person-reported-outcomes'
                                             '-in-boys-with-duchenne-muscular-dystrophy-and-'
                                             'typically-developing-controls-longitudinal-'
                                             'comparisons-and-clinically-meaningful-changes'
                                             '-over-one-year',
                                           'md-13-0002-undiagnosed-genetic-muscle-disease-'
                                             'in-the-north-of-england-an-in-depth-phenotype-analysis',
                                           'proof-of-concept-of-the-ability-of-the-kinect-to-'
                                              'quantify-upper-extremity-function-in-dystrophinopathy',
                                           'guidance-in-social-and-ethical-issues-related-to-clinical',
                                             '-diagnostic-care-and-novel-therapies-for-hereditary-'
                                             'neuromuscular-rare-diseases-translating-the-translational-r',
                                           'a-method-to-produce-and-purify-recombinant-full-length-'
                                             'recombinant-alpha-dystroglycan-analysis-of-n-and-o-linked-'
                                             'monosaccharide-composition-in-cho-cells-with-or-without'
                                             '-large-overexpression',
                                           'the-effect-of-6-thioguanine-on-alternative-splicing-and'
                                             '-antisense-mediated-exon-skipping-treatment-for-duchenne'
                                             '-muscular-dystrophy',
                                           'the-c2a-domain-in-dysferlin-is-important-for-'
                                             'association-with-mg53-trim72',
                                           'md-12-0001-the-effects-of-glucocorticoid-and-voluntary'
                                             '-exercise-treatment-on-the-development-of-thoracolumbar'
                                             '-kyphosis-in-dystrophin-deficient-mice',
                                           'a-proteasome-inhibitor-fails-to-attenuate-dystrophic'
                                             '-pathology-in-mdx-mice',
                                           'clinic-based-infant-screening-for-duchenne-muscular-'
                                             'dystrophy-a-feasibility-study',
                                           'restoration-of-dystrophin-expression-using-the-'
                                             'sleeping-beauty-transposon-2',
                                           'duchenneconnect-registry-report',
                                           'dysferlin-deficient-immortalized-human-qonfe5xowirq-1',
                                           'repression-of-nuclear-celf-activity-can-2zjeyqiqy00gx-1',
                                           'voluntary-wheel-running-in-dystrophin-n0lyxyqck2il-2',
                                           'percent-predicted-6-minute-walk-3vct9230jm3zg-1',
                                           'human-satellite-cells-identification-on-1q26mf55j8ydd-8',
                                           'alterations-in-the-expression-of-g7m7obkpy2dc-1'
                                           'the-proteasomal-inhibitor-mg132-zukachkaw5cv-1',
                                           'reprogramming-efficiency-and-quality-of-2jll4uai3yo54-3',
                                           'intramuscular-transplantation-of-muscle-233il8ybwv3xe-2',
                                           'speckle-tracking-analysis-of-the-left-ventricular-anterior'
                                             '-wall-shows-significantly-decreased-relative-radial-'
                                             'strain-patterns-in-dystrophin-deficient-mice-after-'
                                             '9-months-of-age-2',
                                           'the-different-impact-of-a-high-fat-diet-on-'
                                             'dystrophic-mdx-and-control-c57bl10-mice',
                                        ],
                           'outbreaks' :[
                                           'no-evidence-of-significant-levels-of-toxigenic'
                                             '-v-cholerae-o1-in-the-haitian-aquatic-environment'
                                             '-during-the-2012-rainy-season',
                                           'assessing-risk-for-the-international-spread-of-middle'
                                             '-east-respiratory-syndrome-in-association-with-mass'
                                             '-gatherings-in-saudi-arabia',
                                           'forecasting-peaks-of-seasonal-influenza-epidemics',
                                           'the-dry-season-in-haiti-a-window-of-opportunity'
                                             '-to-eliminate-cholera',
                                           'estimating-human-cases-of-avian-influenza-ah7n9'
                                             '-from-poultry-exposure',
                                           'plos-currents-influenza-archive',
                                           'plos-currents-outbreaks-for-findings-that'
                                             '-the-world-just-cant-wait-to-see',
                                        ],
                           'treeoflife' :
                                        [
                                           'next-generation-phenomics-for-the-tree-of-life',
                                           'arbor-comparative-analysis-workflows-for-the-tree-of-life',
                                           'the-tree-of-life-and-a-new-classification-of-bony-fishes',
                                           'multi-locus-phylogenetic-analysis-reveals-the-pattern'
                                              '-and-tempo-of-bony-fish-evolution',
                                           'phylogenetic-analysis-of-six-domain-multi-copper-blue-proteins',
                                           'the-ideas-lab-concept-assembling-the-tree-of-life-and-avatol',
                                           'an-algorithm-for-calculating-the-probability-of-'
                                             'classes-of-data-patterns-on-a-genealogy',
                                           'standard-maximum-likelihood-analyses-of-alignments-'
                                             'with-gaps-can-be-statistically-inconsistent',
                                           'phylogenetic-discordance-of-human-and-canine-'
                                             'carcinoembryonic-antigen-cea-ceacam-families'
                                             '-but-striking-identity-of-the-cea-receptors-'
                                             'will-impact-comparative-oncology-studies',
                                           'neotropical-and-north-american-2cqluq6cch6gh-5',
                                           'cocos-constructing-multi-domain-protein-phylogenies',
                                           'resolving-the-phylogenetic-and-30uj55ytkepk4-16',
                                           'molecular-data-and-ploidal-levels-indicate-several-'
                                             'putative-allopolyploidization-events-in-the'
                                             '-genus-potentilla-rosaceae',
                                           'overcoming-the-effects-of-rogue-taxa-2cn7m3af919c4-1',
                                           'linking-ncbi-to-wikipedia-a-wiki-based-16h5bb3g3ntlu-2',
                                           'increased-population-sampling-confirms-low-genetic-'
                                             'divergence-among-pteropus-chiroptera-pteropodidae-'
                                             'fruit-bats-of-madagascar-and-other-western-indian-'
                                             'ocean-islands',
                                           'hal-an-automated-pipeline-for-phylogenetic-analyses'
                                             '-of-genomic-data',
                                           'a-time-calibrated-species-level-3chrbtx927cxs-5',
                                           'multiple-sequence-alignment-a-major-challenge-to-large-scale-phylogenetics',
                                           'new-insights-into-the-phylogeny-and-historical-biogeography-of-'
                                             'the-lellingeria-myosuroides-clade-polypodiaceae',
                                           'are-node-based-and-stem-based-clades-equivalent-insights-from-graph-theory',
                                           'benchmark-datasets-and-software-for-developing-and-testing-'
                                             'methods-for-large-scale-multiple-sequence-alignment-and-'
                                             'phylogenetic-inference'
                                        ]

                         }


class Currents():
    """
    """
    _XML_GET_URL_TMPLT = u'{server}/{area}/article/{name}/xml'

    def __init__(self, server=None, area=None, verify=False): 
        self.server = server
        self.area = area
        self.verify = verify

    def _doGet(self, url):
        """
        Requests for Humans not so human after all.
        The verfiy parameter fails if the URL is not https:(
        """
        if url.lower().startswith('https:'):
            return requests.get(url, verify=self.verify)
        else:
            return requests.get(url)

    def getXML(self, area, articleNames):
        """
        """
        for name in articleNames:
            url = self._XML_GET_URL_TMPLT.format(server=self.server, area=area, name=name)
            r = self._doGet(url)
            if r.status_code == 200:
                yield etree.parse(r.raw)
        return 

    def doi(self, area, articleNames):
        """
        """
        for tree in self.getXML(area, articleNames):
            nodes = tree.xpath('//article-id[@pub-id-type="doi"]')
            for node in nodes:
                yield node.text
        return 

    def doiAll(self):
        """
        """
        rslt = {}
        for k,v in CURRENTS_ARTICLES.iteritems():
            rslt[k] = [ doi for doi in self.doi(k, v)]
        return rslt
        

if __name__ == "__main__":
    import argparse   
    import pprint 
 
    # Main command dispatcher.
    dispatch = { 'get' : lambda currents, articleNames: [ etree.tostring(tree, pretty_print=True) 
                           for tree in currents.getXML(articleNames) ], 
                 'doi' : lambda currents, articleNames: [ tag for tag in currents.doi(articleNames) ],
                 'doiAll' : lambda currents, articleNames: [ currents.doiAll() ]   }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='PLOS Currents Command Line Interface.')
    parser.add_argument('--server', default='http://currents.plos.org', help='specify a Currents server url.')
    parser.add_argument('--area', default='disasters', help='disasters, genomictests, hd, md, outbreaks, treeoflife' )
    parser.add_argument('command', help="get, doi, doiAll")
    parser.add_argument('articleList', nargs='*', help="list of Currents articles.")
    args = parser.parse_args()

    try:
        for val in dispatch[args.command](Currents(server=args.server, area=args.area), args.articleList):
            pp.pprint(val)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)

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
u'disasters': [ ( u'non-communicable-diseases-in-emergencies-a-call-to-action',
                    '10.1371/currents.dis.53e08b951d59ff913ab8b9bb51c4d0de'),
                  ( u'aftershocks-following-the-9-april-2013-bushehr-earthquake-iran',
                    '10.1371/currents.dis.76750ede500e61b81d7f2ba9edfb2373'),
                  ( u'a-prospective-study-of-the-outcome-of-patients-with-limb-'
                       'trauma-following-the-haitian-earthquake-in-2010-at-one-and'
                       '-two-year-the-sutra2-study-2',
                    '10.1371/currents.dis.931c4ba8e64a95907f16173603abb52f'),
                  ( u'a-prospective-study-of-the-outcome-of-patients-with-limb'
                       '-trauma-following-the-haitian-earthquake-in-2010-at-one'
                       '-and-two-year-the-sutra2-study-2',
                    '10.1371/currents.dis.931c4ba8e64a95907f16173603abb52f'),
                  ( u'twitter-as-a-sentinel-in-emergency-situations-lessons-'
                       'from-the-boston-marathon-explosions',
                    '10.1371/currents.dis.ad70cd1c8bc585e9470046cde334ee4b'),
                  ( u'impacts-of-natural-hazards-on-primary-health-care-facilitiesi'
                       '-of-iran-a-10-year-retrospective-survey',
                    '10.1371/currents.dis.ccdbd870f5d1697e4edee5eda12c5ae6'),
                  ( u'which-anthropometric-indicators-identify-a-pregnant-woman'
                       '-as-acutely-malnourished-and-predict-adverse-birth-'
                       'outcomes-in-the-humanitarian-context',
                    '10.1371/currents.dis.54a8b618c1bc031ea140e3f2934599c8'),
                  ( u'social-factors-as-modifiers-of-hurricane-irene-evacuation'
                       '-behavior-in-beaufort-county-nc',
                    '10.1371/currents.dis.620b6c2ec4408c217788bb1c091ef919'),
                  ( u'dis-13-0001-health-effects-of-drought-a-systematic-review'
                       '-of-the-evidence',
                    '10.1371/currents.dis.7a2cee9e980f91ad7697b570bcc4b004'),
                  ( u'the-great-east-japan-earthquake-disaster-a-compilation-'
                       'of-published-literature-on-health-needs-and-relief-activities'
                       '-march-2011-september-2012',
                    '10.1371/currents.dis.771beae7d8f41c31cd91e765678c005d'),
                  ( u'the-great-east-japan-earthquake-disaster-a-compilation-'
                       'of-published-literature-on-health-needs-and-relief-'
                       'activities-march-2011-september-2012',
                    '10.1371/currents.dis.771beae7d8f41c31cd91e765678c005d'),
                  ( u'lessons-from-a-flash-flood-in-tehran-subway-iran',
                    '10.1371/currents.dis.b80efb4a82b7bc2b9dbb5280d79da497'),
                  ( u'irans-bushehr-earthquake-at-a-glance',
                    '10.1371/currents.dis.b69b729791d032b6a1e0f5f9ac4571a4'),
                  ( u'vulnerabilities-of-local-healthcare-providers-in-complexi'
                       '-emergencies-findings-from-the-manipur-micro-level-'
                       'insurgency-database-2008-2009',
                    '10.1371/currents.dis.397bcdc6602b84f9677fe49ee283def7'),
                  ( u'state-of-virtual-reality-vr-based-disaster-preparedness'
                       '-and-response-training',
                    '10.1371/currents.dis.1ea2b2e71237d5337fa53982a38b2aff'),
                  ( u'interventions-to-mitigate-emergency-department-and-hospital'
                       '-crowding-during-an-infectious-respiratory-disease-'
                       'outbreak-results-from-an-expert-panel',
                    '10.1371/currents.dis.1f277e0d2bf80f4b2bb1dd5f63a13993'),
                  ( u'the-human-impacts-of-tsunamis-a-historical-review-of-'
                       'events-1900-2009-and-systematic-literature-review',
                    '10.1371/currents.dis.40f3c5cf61110a0fef2f9a25908cd795'),
                  ( u'the-human-impact-of-volcanoes-a-historical-review-of-'
                       'events-1900-2009-and-systematic-literature-review',
                    '10.1371/currents.dis.841859091a706efebf8a30f4ed7a1901'),
                  ( u'the-human-impact-of-floods-a-historical-review-of-events'
                       '-1980-2009-and-systematic-literature-review',
                    '10.1371/currents.dis.f4deb457904936b07c09daa98ee8171a'),
                  ( u'the-human-impact-of-earthquakes-from-1980-2009-a-historical'
                       '-review-of-events-1980-2009-and-systematic-literature-review',
                    '10.1371/currents.dis.67bd14fe457f1db0b5433a8ee20fb833'),
                  ( u'the-human-impact-of-tropical-cyclones-a-historical-'
                       'review-of-events-1980-2009-and-systematic-literature-review',
                    '10.1371/currents.dis.2664354a5571512063ed29d25ffbce74'),
                  ( u'dis-13-0009-a-summary-case-report-on-the-health-impacts'
                       '-and-response-to-the-pakistan-floods-of-2010',
                    '10.1371/currents.dis.cc7bd532ce252c1b740c39a2a827993f'),
                  ( u'a-framework-and-methodology-for-navigating-disaster-and'
                       '-global-health-in-crisis-literature',
                    '10.1371/currents.dis.9af6948e381dafdd3e877c441527cba0'),
                  ( u'housing-reconstruction-in-disaster-recovery-a-study-of-'
                       'fishing-communities-post-tsunami-in-chennai-india',
                    '10.1371/currents.dis.a4f34a96cb91aaffacd36f5ce7476a36'),
                  ( u'spinal-injuries-in-the-2012-twin-earthquakes-northwest-iran',
                    '10.1371/currents.dis.39b14d88c93fe04ef1a2ce180b24f8d1'),
                  ( u'dis-12-0017-the-use-of-systematic-reviews-and-other-research'
                       '-evidence-in-disasters-and-related-areas-preliminary-'
                       'report-of-a-needs-assessment-survey',
                    '10.1371/currents.dis.ed42382881b3bf79478ad503be4693ea'),
                  ( u'dis-12-0009-mortality-in-the-laquila-central-italy-earthquake'
                       '-of-6-april-2009',
                    '10.1371/50585b8e6efd1'),
                  ( u'public-health-surveillance-after-the-2010-haiti-earthquake'
                       '-the-experience-of-medecins-sans-frontieres',
                    '10.1371/currents.dis.6aec18e84816c055b8c2a06456811c7a'),
                  ( u'dis-12-0012-the-role-of-veterans-affairs-in-emergency-'
                       'management-a-systematic-literature-review',
                    '10.1371/198d344bc40a75f927c9bc5024279815'),
                  ( u'lessons-from-the-recent-twin-earthquakes-in-iran',
                    '10.1371/currents.dis.ea574d0075a8e90a9cb782b368c60c36'),
                  (u'health-impacts-of-wildfires', '10.1371/4f959951cce2c'),
                  ( u'dis-12-0013-secondary-stressors-and-extreme-events-and'
                       '-disasters-a-systematic-review-of-primary-research-from-2010-2011',
                    '10.1371/currents.dis.a9b76fed1b2dd5c5bfcfc13c87a2f24f'),
                  ( u'dis-12-0006-repeat-triage-in-disaster-relief-questions-from-haiti',
                    '10.1371/4fbbdec6279ec'),
                  ( u'dis-12-0007-assessing-the-impact-of-workshops-promoting-'
                       'concepts-of-psychosocial-support-for-emergency-events',
                    '10.1371/4fd80324dd362'),
                  ( u'dis-12-0003-the-haitian-health-cluster-experience-a-comparative'
                       '-evaluation-of-the-professional-communication-response-to-the-'
                       '2010-earthquake-and-the-subsequent-cholera-outbreak',
                    '10.1371/5014b1b407653'),
                  ( u'developing-a-health-system-approach-to-disaster-management-a-'
                       'qualitative-analysis-of-the-core-literature-to-complement-'
                       'the-who-toolkit-for-assessing-health-system-capacity-for-'
                       'crisis-management',
                    '10.1371/5028b6037259a'),
                  ( u'a-health-system-approach-to-all-hazards-disaster-management-'
                       'a-systematic-review',
                    '10.1371/50081cad5861d'),
                  ( u'impact-of-the-2010-pakistan-floods-on-rural-and-urban-populations'
                       '-at-six-months',
                    '10.1371/4fdfb212d2432'),
                  ( u'disaster-metrics-a-proposed-quantitative-assessment-tool-in-'
                       'complex-humanitarian-emergencies-the-public-health-impact-'
                       'severity-scale-phiss',
                    '10.1371/4f7b4bab0d1a3'),
                  ( u'knowledge-attitude-and-practice-of-tehrans-inhabitants-for-an-earthquake-and-related-determinants',
                    '10.1371/4fbbbe1668eef'),
                  ( u'weather-and-environmental-hazards-at-mass-gatherings',
                    '10.1371/4fca9ee30afc4'),
                  ( u'monitoring-the-mental-well-being-of-caregivers-during-the-haiti-earthquake',
                    '10.1371/4fc33066f1947'),
                  ( u'review-of-the-osha-niosh-response-to-the-deepwater-horizon-oil-spill-protecting-the-health-and-safety-of-cleanup-workers',
                    '10.1371/4fa83b7576b6e'),
                  ( u'local-public-health-system-response-to-the-tsunami-threat-in-coastal-california-following-the-tohoku-earthquake',
                    '10.1371/4f7f57285b804'),
                  ( u'how-to-use-near-real-time-health-indicators-to-support-decision-making-during-a-heat-wave-the-example-of-the-french-heat-wave-warning-system',
                    '10.1371/4f83ebf72317d'),
                  ( u'2012-2025-roadmap-of-i-r-irans-disaster-health-management',
                    '10.1371/4f93005fbcb34'),
                  ( u'the-good-the-bad-and-the-ugly-disaster-risk-reduction-drr-versus-disaster-risk-creation-drc',
                    '10.1371/4f8d4eaec6af8'),
                  ( u'the-effects-of-flooding-on-mental-health-outcomes-and-recommendations-from-a-review-of-the-literature',
                    '10.1371/4f9f1fa9c3cae'),
                  ( u'timing-and-type-of-disaster-severity-data-available-on-internet-following-the-2010-haiti-earthquake',
                    '10.1371/4fb3fd97a2d3c'),
                  ( u'the-great-east-japan-earthquake-experiences-and-suggestions-for-survivors-with-diabetes-perspective-2',
                    '10.1371/4facf9d99b997'),
                  ( u'utstein-style-template-for-uniform-data-reporting-of-acute-medical-response-in-disasters',
                    '10.1371/4f6cf3e8df15a'),
                  ( u'cholera-ante-portas-the-re-emergence-of-cholera-in-kinshasa-after-a-ten-year-hiatus',
                    '10.1371/currents.RRN1310'),
                  ( u'quantification-of-the-heat-wave-effect-on-mortality-in-nine-french-cities-during-summer-2006-2',
                    '10.1371/currents.RRN1307'),
                  ( u'the-buncefield-oil-depot-fire-of-2005-3wkcplftb6ss-3',
                    '10.1371/currents.RRN1300'),
                  ( u'disasters-at-mass-gatherings-lessons-3wkcplftb6ss-5',
                    '10.1371/currents.RRN1301'),
                  ( u'the-dadaab-camps-mitigating-the-effects-2uh9ieg4z57f8-6',
                    '10.1371/currents.RRN1289'),
                  ( u'aspirations-and-compromises-changes-in-2ktejo3jcihdk-1',
                    '10.1371/currents.RRN1280'),
                  ( u'the-role-of-collective-action-in-2z8p4t1bp5hj5-2',
                    '10.1371/currents.RRN1279'),
                  ( u'evidence-for-disaster-risk-reduction-9z2b6qmqlx3z-3',
                    '10.1371/currents.RRN1270'),
                  ( u'dealing-with-disaster-databases-what-2q9epyi1nr2rp-2',
                    '10.1371/currents.RRN1272'),
                  ( u'valuing-lives-allocating-scarce-medical-2c8q7lnatgpgj-2',
                    '10.1371/currents.RRN1271')],
u'genomictests': [ 
                     ( u'genetic-testing-strategies-in-newly-diagnosed-endometrial-'
                          'cancer-patients-aimed-at-reducing-morbidity-or-mortality'
                          '-from-lynch-syndrome-in-the-index-case-or-her-relatives',
                       '10.1371/currents.eogt.b59a6e84f27c536e50db4e46aa26309c'),
                     ( u'use-of-the-corus-cad-gene-expression-test-for-assessment'
                          '-of-obstructive-coronary-artery-disease-likelihood-in'
                          '-symptomatic-non-diabetic-patients',
                       '10.1371/currents.eogt.0f04f6081905998fa92b99593478aeab'),
                     ( u'scn1a-genetic-test-for-dravet-syndrome-severe-myoclonic-'
                          'epilepsy-of-infancy-and-its-clinical-subtypes',
                       '10.1371/currents.eogt.c553b83d745dd79bfb61eaf35e522b0b'),
                     ( u'the-decisiondx-um-gene-expression-profile-test-provides-'
                          'risk-stratification-and-individualized-patient-care-in-uveal-melanoma-2',
                       '10.1371/currents.eogt.af8ba80fc776c8f1ce8f5dc485d4a618'),
                     ( u'use-of-the-afirma-gene-expression-classifier-for-preoperative'
                          '-identification-of-benign-thyroid-nodules-with-indeterminate'
                          '-fine-needle-aspiration-cytopathology',
                       '10.1371/currents.eogt.e557cbb5c7e4f66568ce582a373057e7'),
                     ( u'genetic-testing-for-long-qt-syndrome-and-the-category-of-cardiac-ion-channelopathies',
                       '10.1371/4f9995f69e6c7'),
                     ( u'comprehensive-carrier-screening-and-molecular-diagnostic'
                          '-testing-for-recessive-childhood-diseases',
                       '10.1371/4f9877ab8ffa9'),
                     ( u'a-20-gene-model-for-predicting-nodal-qjiio56ycgdt-4',
                       '10.1371/currents.RRN1248'),
                     ( u'use-of-oncotype-dx-in-women-with-node-positive-breast-cancer',
                       '10.1371/currents.RRN1249'),
                     ( u'use-of-epidermal-growth-factor-receptor-mutation-analysis'
                          '-in-patients-with-advanced-non-small-cell-lung-cancer-to-'
                          'determine-erlotinib-use-as-first-line-therapy',
                       '10.1371/currents.RRN1245'),
                     ( u'genetic-testing-for-lynch-syndrome-in-individuals-newly-diagnosed'
                          '-with-colorectal-cancer-to-reduce-morbidity-and-mortality-from'
                          '-colorectal-cancer-in-their-relatives',
                       '10.1371/currents.RRN1246'),
                     ( u'cascade-screening-for-familial-70fnx9tmvdav-13',
                       '10.1371/currents.RRN1238'),
                     ( u'thiopurine-methyltransferase-tpmt-xv5k9xg3o4yu-8',
                       '10.1371/currents.RRN1236'),
                     ( u'fecal-dna-testing-for-colorectal-cancer-od1hzthyodr3-1',
                       '10.1371/currents.RRN1220'),
                     ( u'genetic-risk-profiling-for-prediction-20113liwenx5c-9',
                       '10.1371/currents.RRN1208'),
                     ( u'interleukin-28b-genotype-testing-to-xv5k9xg3o4yu-3',
                       '10.1371/currents.RRN1207'),
                     ( u'bcr-abl-mutation-testing-to-predict-response-to-tyrosine-kinase-inhibitors-in-patients-with-chronic-myeloid-leukemia',
                       '10.1371/currents.RRN1204'),
                     ( u'hla-b-5701-testing-to-predict-abacavir-2twojvq0wfutb-1',
                       '10.1371/currents.RRN1203'),
                     ( u'ercc1-expression-analysis-to-guide-1i0b298tuv2uk-4',
                       '10.1371/currents.RRN1202'),
                     ( u'kif6-p-trp719arg-testing-to-assess-risk-1i0b298tuv2uk-3',
                       '10.1371/currents.RRN1191'),
                     ( u'braf-p-val600glu-v600e-testing-for-assessment-of-treatment-options-in-metastatic-colorectal-cancer',
                       '10.1371/currents.RRN1187'),
                     ( u'decisiondx-gbm-gene-expression-assay-for-prognostic-testing-in-glioblastoma-multiform',
                       '10.1371/currents.RRN1186'),
                     ( u'cyp2d6-testing-to-predict-response-to-3qgx0fg7r52v-1',
                       '10.1371/currents.RRN1176'),
                     ( u'genetic-testing-for-cyp450-70fnx9tmvdav-1',
                       '10.1371/currents.RRN1180'),
                     ( u'testing-of-vkorc1-and-cyp2c9-alleles-to-x2fzi0wvbcnu-1',
                       '10.1371/currents.RRN1155'),
                     ( u'kras-mutational-analysis-for-colorectal-27yi6810q97hp-1',
                       '10.1371/currents.RRN1175'),
                     ( u'oncotype-dx-tumor-gene-expression-profiling-in-stage-ii-colon-cancer',
                       '10.1371/currents.RRN1177'),
                     ( u'tumor-gene-expression-profiling-in-women-with-breast-cancer',
                       '10.1371/currents.RRN1178'),
                     ( u'plos-currents-evidence-on-genomic-tests-at-the-crossroads-of-translation',
                       '10.1371/currents.RRN1179')],
  u'hd': [ 
           ( u'increased-body-weight-of-the-bac-hd-transgenic-mouse-model'
                '-of-huntingtons-disease-accounts-for-some-but-not-all-of-the'
                '-observed-hd-like-motor-deficits',
             '10.1371/currents.hd.0ab4f3645aff523c56ecc8ccbe41a198'),
           ( u'high-throughput-automated-phenotyping-of-two-genetic-mouse-models-of-huntingtons-disease',
             '10.1371/currents.hd.124aa0d16753f88215776fba102ceb29'),
           ( u'is-a-motor-criterion-essential-for-the-diagnosis-of-clinical-huntington-disease',
             '10.1371/currents.hd.f4c66bd51e8db11f55e1701af937a419'),
           ( u'dysfunctional-mitochondrial-respiration-in-the-striatum-of-the-huntingtons-disease-transgenic-r62-mouse-model',
             '10.1371/currents.hd.d8917b4862929772c5a2f2a34ef1c201'),
           ( u'effects-of-the-pimelic-diphenylamide-histone-deacetylase'
                 '-inhibitor-hdaci-4b-on-the-r62-and-n171-82q-mouse-models-of-huntingtons-disease',
             '10.1371/currents.hd.ec3547da1c2a520ba959ee7bf8bdd202'),
           ( u'hd-12-0003-pitfalls-in-the-detection-of-cholesterol-in-huntingtons-disease-models',
             '10.1371/505886e9a1968'),
           ( u'the-prevalence-of-juvenile-huntingtons-disease-a-review-of-the-literature-and-meta-analysis',
             '10.1371/4f8606b742ef3'),
           ( u'caspase-6-does-not-contribute-to-the-proteolysis-of-'
                'mutant-huntingtin-in-the-hdhq150-knock-in-mouse-model-of-huntingtons-disease',
             '10.1371/4fd085bfc9973'),
           ( u'intrastriatal-transplantation-of-neurotrophic-factor-secreting-human'
                '-mesenchymal-stem-cells-improves-motor-function-and-extends-'
                'survival-in-r62-transgenic-mouse-model-for-huntingtons-disease',
             '10.1371/4f7f6dc013d4e'),
           ( u'protection-by-glia-conditioned-medium-in-a-cell-model-of-huntington-disease',
             '10.1371/4fbca54a2028b'),
           ( u'a-mixed-fixed-ratioprogressive-ratio-procedure-reveals-an-apathy'
                '-phenotype-in-the-bac-hd-and-the-z_q175-ki-mouse-models-of-huntingtons-disease',
             '10.1371/4f972cffe82c0'),
           ( u'pharmacokinetics-of-memantine-in-rats-and-mice',
             '10.1371/currents.RRN1291'),
           ( u'effect-of-the-rd1-mutation-on-motor-performance-in-r62-and-wild-type-mice',
             '10.1371/currents.RRN1303'),
           ( u'huntingtons-like-conditions-in-china-a-review-of-published-chinese-cases-2',
             '10.1371/currents.RRN1302'),
           ( u'current-pharmacological-management-in-juvenile-huntingtons-disease-2',
             '10.1371/currents.RRN1304'),
           ( u'stability-effects-on-results-of-diffusion-tensor-imaging-analysis-by'
                '-reduction-of-the-number-of-gradient-directions-due-to-motion-artifacts'
                '-an-application-to-presymptomatic-huntingtons-disea',
             '10.1371/currents.RRN1292'),
           ( u'aspiration-pneumonia-and-death-in-huntingtons-disease-2',
             '10.1371/currents.RRN1293'),
           ( u'seven-year-clinical-follow-up-of-premanifest-carriers-of-huntingtons-disease-2',
             '10.1371/currents.RRN1288'),
           ( u'no-evidence-of-impaired-gastric-emptying-in-early-huntingtons-disease',
             '10.1371/currents.RRN1284'),
           ( u'use-of-tetrabenazine-in-huntington-disease-patients-on-antidepressants'
                '-or-with-advanced-disease-results-from-the-tetra-hd-study',
             '10.1371/currents.RRN1283'),
           ( u'hd-mouse-models-reveal-clear-deficits-in-learning-to-perform-a-simple-instrumental-response',
             '10.1371/currents.RRN1282'),
           ( u'effect-of-enhanced-voluntary-physical-exercise-on-brain-levels-of-monoamines-in-huntington-disease-mice',
             '10.1371/currents.RRN1281'),
           ( u'longitudinal-change-in-gait-and-motor-23cuk2jhasmoq-3',
             '10.1371/currents.RRN1268'),
           ( u'baroreceptor-reflex-dysfunction-in-the-bachd-mouse-model-of-huntingtons-disease',
             '10.1371/currents.RRN1266'),
           ( u'assessment-of-day-to-day-functioning-in-prodromal-and-early-huntington-disease-3',
             '10.1371/currents.RRN1262'),
           ( u'an-international-survey-based-algorithm-for-the-pharmacologic-treatment-of-irritability-in-huntingtons-disease',
             '10.1371/currents.RRN1259'),
           ( u'an-international-survey-based-algorithm-for-the-pharmacologic-treatment-of-chorea-in-huntingtons-disease',
             '10.1371/currents.RRN1260'),
           ( u'an-international-survey-based-algorithm-p284k2gmahk5-9',
             '10.1371/currents.RRN1261'),
           ( u'modifiers-of-mutant-huntingtin-2guskb0m0ddva-1',
             '10.1371/currents.RRN1255'),
           ( u'self-reports-of-day-to-day-function-in-a-small-cohort-of-people-with-prodromal-and-early-hd',
             '10.1371/currents.RRN1254'),
           ( u'music-perception-and-movement-deterioration-in-huntingtons-disease',
             '10.1371/currents.RRN1252'),
           ( u'assessment-of-cognitive-symptoms-in-prodromal-and-early-huntington-disease',
             '10.1371/currents.RRN1250'),
           ( u'age-at-onset-in-huntington-disease',
             '10.1371/currents.RRN1258'),
           ( u'nmda-receptor-gene-variations-as-modifiers-in-huntington-disease-a-replication-study',
             '10.1371/currents.RRN1247'),
           ( u'assessment-of-motor-symptoms-and-functional-impact-in-prodromal-and-early-huntington-disease',
             '10.1371/currents.RRN1244'),
           ( u'assessing-behavioural-manifestations-prior-to-clinical-diagnosis-of-huntington'
                '-disease-anger-and-irritability-and-obsessions-and-compulsions-2',
             '10.1371/currents.RRN1241'),
           ( u'assessment-of-depression-anxiety-and-apathy-in-prodromal-and-early-huntington-disease-2',
             '10.1371/currents.RRN1242'),
           ( u'stability-of-white-matter-changes-related-to-huntingtons-disease-in-the-presence-of-'
                'imaging-noise-a-dti-study',
             '10.1371/currents.RRN1232'),
           ( u'association-between-age-and-striatal-volume-stratified-by-cag-repeat-length-in-'
                'prodromal-huntington-disease',
             '10.1371/currents.RRN1235'),
           ( u'abnormal-peripheral-chemokine-profile-in-huntingtons-disease',
             '10.1371/currents.RRN1231'),
           ( u'advocacy-recruiting-for-huntington-s-p284k2gmahk5-2',
             '10.1371/currents.RRN1230'),
           ( u'body-composition-in-premanifest-huntingtons-disease-reveals-lower-bone-density-compared-to-controls',
             '10.1371/currents.RRN1214'),
           ( u'huntingtin-localisation-studies-a-2co1l1tasmbx7-3',
             '10.1371/currents.RRN1211'),
           ( u'visualization-of-cell-to-cell-2sdo8o1u01fbj-1',
             '10.1371/currents.RRN1210'),
           ( u'circadian-abnormalities-in-motor-activity-in-a-bac-transgenic-mouse-model-of-huntingtons-disease',
             '10.1371/currents.RRN1225'),
           ( u'optimization-of-an-htrf-assay-for-the-3p3s60imu228g-2',
             '10.1371/currents.RRN1205'),
           ( u'utilisation-of-healthcare-and-associated-services-in-huntingtons-disease-a-data-mining-study-2',
             '10.1371/currents.RRN1206'),
           ( u'exercise-is-not-beneficial-and-may-3jzbm8k1knz4c-2',
             '10.1371/currents.RRN1201'),
           ( u'characterization-of-human-huntingtons-disease-cell-model-from-induced-pluripotent-stem-cells-2',
             '10.1371/currents.RRN1193'),
           ( u'qeeg-measures-in-huntingtons-disease-a-pilot-study',
             '10.1371/currents.RRN1192'),
           ( u'serum-levels-of-a-subset-of-cytokines-show-high-interindividual-variability-and-are-not-altered'
                '-in-rats-transgenic-for-huntington\xb4s-disease',
             '10.1371/currents.RRN1190'),
           ( u'identifying-sleep-disturbances-in-huntingtons-disease-using-a-simple-disease-focused-questionnaire',
             '10.1371/currents.RRN1189'),
           ( u'drosophila-models-of-huntington-s-2as2tsd09zfs8-2',
             '10.1371/currents.RRN1185'),
           ( u'observing-huntingtons-disease-the-european-huntingtons-disease-networks-registry-3',
             '10.1371/currents.RRN1184'),
           ( u'transplantation-of-patient-derived-adipose-stem-cells-in-yac128-huntingtons-disease-transgenic-mice',
             '10.1371/currents.RRN1183'),
           ( u'rescuing-the-corticostriatal-synaptic-disconnection-in-the-r62-mouse-model-of-huntingtons-disease-'
                'exercise-adenosine-receptors-and-ampakines',
             '10.1371/currents.RRN1182'),
           ( u'evaluation-of-histone-deacetylases-as-drug-targets-in-huntingtons-disease-models-2',
             '10.1371/currents.RRN1172'),
           ( u'cloned-mri-t2-hypointensities-in-basal-ganglia-of-premanifest-huntingtons-disease',
             '10.1371/currents.RRN1173'),
           ( u'cognitive-follow-up-of-a-small-cohort-of-huntingtons-disease-patients-over-a-5-year-period',
             '10.1371/currents.RRN1174')],
  u'md': [ 
           ( u'generation-of-embryonic-stem-cells-and-mice-for-duchenne-research',
             '10.1371/currents.md.cbf1d33001de80923ce674302cad7925'),
           ( u'stem-cell-antigen-1-in-skeletal-muscle-function',
             '10.1371/currents.md.411a8332d61e22725e6937b97e6d0ef8'),
           ( u'the-6-minute-walk-test-and-person-reported-outcomes-in-boys-with-'
                'duchenne-muscular-dystrophy-and-typically-developing-controls-longitudinal-comparisons'
                '-and-clinically-meaningful-changes-over-one-year',
             '10.1371/currents.md.9e17658b007eb79fcd6f723089f79e06'),
           ( u'md-13-0002-undiagnosed-genetic-muscle-disease-in-the-north-of-england-an-in-depth-phenotype-analysis',
             '10.1371/currents.md.37f840ca67f5e722945ecf755f40487e'),
           ( u'proof-of-concept-of-the-ability-of-the-kinect-to-quantify-upper-extremity-function-in-dystrophinopathy',
             '10.1371/currents.md.9ab5d872bbb944c6035c9f9bfd314ee2'),
           ( u'a-method-to-produce-and-purify-recombinant-full-length-recombinant-alpha-dystroglycan-analysis-of-n-and'
                '-o-linked-monosaccharide-composition-in-cho-cells-with-or-without-large-overexpression',
             '10.1371/currents.md.3756b4a389974dff21c0cf13508d3f7b'),
           ( u'the-effect-of-6-thioguanine-on-alternative-splicing-and-antisense-mediated-exon-skipping-'
                'treatment-for-duchenne-muscular-dystrophy',
             '10.1371/currents.md.597d700f92eaa70de261ea0d91821377'),
           ( u'the-c2a-domain-in-dysferlin-is-important-for-association-with-mg53-trim72',
             '10.1371/5035add8caff4'),
           ( u'md-12-0001-the-effects-of-glucocorticoid-and-voluntary-exercise-treatment-on-the-development'
                '-of-thoracolumbar-kyphosis-in-dystrophin-deficient-mice',
             '10.1371/4ffdff160de8b'),
           ( u'a-proteasome-inhibitor-fails-to-attenuate-dystrophic-pathology-in-mdx-mice',
             '10.1371/4f84a944d8930'),
           ( u'clinic-based-infant-screening-for-duchenne-muscular-dystrophy-a-feasibility-study',
             '10.1371/4f99c5654147a'),
           ( u'restoration-of-dystrophin-expression-using-the-sleeping-beauty-transposon-2',
             '10.1371/currents.RRN1296'),
           (u'duchenneconnect-registry-report', '10.1371/currents.RRN1309'),
           ( u'dysferlin-deficient-immortalized-human-qonfe5xowirq-1',
             '10.1371/currents.RRN1298'),
           ( u'repression-of-nuclear-celf-activity-can-2zjeyqiqy00gx-1',
             '10.1371/currents.RRN1305'),
           ( u'voluntary-wheel-running-in-dystrophin-n0lyxyqck2il-2',
             '10.1371/currents.RRN1295'),
           ( u'percent-predicted-6-minute-walk-3vct9230jm3zg-1',
             '10.1371/currents.RRN1297'),
           ( u'human-satellite-cells-identification-on-1q26mf55j8ydd-8',
             '10.1371/currents.RRN1294'),
           ( u'reprogramming-efficiency-and-quality-of-2jll4uai3yo54-3',
             '10.1371/currents.RRN1274'),
           ( u'intramuscular-transplantation-of-muscle-233il8ybwv3xe-2',
             '10.1371/currents.RRN1275'),
           ( u'speckle-tracking-analysis-of-the-left-ventricular-anterior-wall-shows-significantly'
                '-decreased-relative-radial-strain-patterns-in-dystrophin-deficient-mice-after-9-months-of-age-2',
             '10.1371/currents.RRN1273'),
           ( u'the-different-impact-of-a-high-fat-diet-on-dystrophic-mdx-and-control-c57bl10-mice',
             '10.1371/currents.RRN1276')],
  u'outbreaks': [ 
                  ( u'no-evidence-of-significant-levels-of-toxigenic-v-cholerae-o1-in-the'
                       '-haitian-aquatic-environment-during-the-2012-rainy-season',
                    '10.1371/currents.outbreaks.7735b392bdcb749baf5812d2096d331e'),
                  ( u'assessing-risk-for-the-international-spread-of-middle-east-respiratory-'
                       'syndrome-in-association-with-mass-gatherings-in-saudi-arabia',
                    '10.1371/currents.outbreaks.a7b70897ac2fa4f79b59f90d24c860b8'),
                  ( u'forecasting-peaks-of-seasonal-influenza-epidemics',
                    '10.1371/currents.outbreaks.bb1e879a23137022ea79a8c508b030bc'),
                  ( u'the-dry-season-in-haiti-a-window-of-opportunity-to-eliminate-cholera',
                    '10.1371/currents.outbreaks.2193a0ec4401d9526203af12e5024ddc'),
                  ( u'estimating-human-cases-of-avian-influenza-ah7n9-from-poultry-exposure',
                    '10.1371/currents.outbreaks.264e737b489bef383fbcbaba60daf928'),
                  ( u'plos-currents-influenza-archive',
                    '10.1371/currents.outbreaks.945bd1abf1dedfea70887d49165276c7'),
                  ( u'plos-currents-outbreaks-for-findings-that-the-world-just-cant-wait-to-see',
                    '10.1371/currents.outbreaks.8ed218c079fbded60c505f025ed45f67')],
  u'treeoflife': [ ( u'next-generation-phenomics-for-the-tree-of-life',
                     '10.1371/currents.tol.085c713acafc8711b2ff7010a4b03733'),
                   ( u'arbor-comparative-analysis-workflows-for-the-tree-of-life',
                     '10.1371/currents.tol.099161de5eabdee073fd3d21a44518dc'),
                   ( u'the-tree-of-life-and-a-new-classification-of-bony-fishes',
                     '10.1371/currents.tol.53ba26640df0ccaee75bb165c8c26288'),
                   ( u'multi-locus-phylogenetic-analysis-reveals-the-pattern-and-tempo-of-bony-fish-evolution',
                     '10.1371/currents.tol.2ca8041495ffafd0c92756e75247483e'),
                   ( u'phylogenetic-analysis-of-six-domain-multi-copper-blue-proteins',
                     '10.1371/currents.tol.574bcb0f133fe52835911abc4e296141'),
                   ( u'the-ideas-lab-concept-assembling-the-tree-of-life-and-avatol',
                     '10.1371/currents.tol.0fdb85e1619f313a2a5a2ec3d7a8df9e'),
                   ( u'an-algorithm-for-calculating-the-probability-of-classes-of-data-patterns-on-a-genealogy',
                     '10.1371/4fd1286980c08'),
                   ( u'standard-maximum-likelihood-analyses-of-alignments-with-gaps-can-be-statistically-inconsistent',
                     '10.1371/currents.RRN1308'),
                   ( u'phylogenetic-discordance-of-human-and-canine-carcinoembryonic-antigen-cea-ceacam-families-but'
                        '-striking-identity-of-the-cea-receptors-will-impact-comparative-oncology-studies',
                     '10.1371/currents.RRN1223'),
                   ( u'neotropical-and-north-american-2cqluq6cch6gh-5',
                     '10.1371/currents.RRN1227'),
                   ( u'cocos-constructing-multi-domain-protein-phylogenies',
                     '10.1371/currents.RRN1240'),
                   ( u'resolving-the-phylogenetic-and-30uj55ytkepk4-16',
                     '10.1371/currents.RRN1239'),
                   ( u'molecular-data-and-ploidal-levels-indicate-several-putative-allopolyploidization-events'
                        '-in-the-genus-potentilla-rosaceae',
                     '10.1371/currents.RRN1237'),
                   ( u'overcoming-the-effects-of-rogue-taxa-2cn7m3af919c4-1',
                     '10.1371/currents.RRN1233'),
                   ( u'linking-ncbi-to-wikipedia-a-wiki-based-16h5bb3g3ntlu-2',
                     '10.1371/currents.RRN1228'),
                   ( u'increased-population-sampling-confirms-low-genetic-divergence-among-pteropus-chiroptera'
                        '-pteropodidae-fruit-bats-of-madagascar-and-other-western-indian-ocean-islands',
                     '10.1371/currents.RRN1226'),
                   ( u'hal-an-automated-pipeline-for-phylogenetic-analyses-of-genomic-data',
                     '10.1371/currents.RRN1213'),
                   ( u'a-time-calibrated-species-level-3chrbtx927cxs-5',
                     '10.1371/currents.RRN1212'),
                   ( u'multiple-sequence-alignment-a-major-challenge-to-large-scale-phylogenetics',
                     '10.1371/currents.RRN1198'),
                   ( u'new-insights-into-the-phylogeny-and-historical-biogeography-of-the-lellingeria-myosuroides-clade-polypodiaceae',
                     '10.1371/currents.RRN1197'),
                   ( u'are-node-based-and-stem-based-clades-equivalent-insights-from-graph-theory',
                     '10.1371/currents.RRN1196'),
                   ( u'benchmark-datasets-and-software-for-developing-and-testing-methods-for-large-scale-multiple-sequence-alignment-and-phylogenetic-inference',
                     '10.1371/currents.RRN1195')]}
             
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

    def _getBinary(self, fname, url):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5
        hash of the fle contents.
        """
        m = md5.new()
        r = self._doGet(url)
        if r.status_code == 200:
            with open(fname, 'wb') as f:
                for chunk in r.iter_content(1024):
                    m.update(chunk)
                    f.write(chunk)
                f.close()
        else:
            raise Exception('rhino:failed to get binary ' + url)
        return m.hexdigest()

    def getXML(self, articleNames, area=None):
        """
        """
        if area == None:
           area = self.area
 
        for name in articleNames:
            url = self._XML_GET_URL_TMPLT.format(server=self.server, area=area, name=name)
            r = self._doGet(url)
            if r.status_code == 200:
                yield (name, etree.parse(r.raw))
        return 

    def doi(self, articleNames, area=None):
        """
        """
        if area == None:
           area = self.area

        for (name, tree) in self.getXML(articleNames, area=area):
            nodes = tree.xpath('//article-id[@pub-id-type="doi"]')
            for node in nodes:
                yield (name, node.text)
        return

    def media(self, articleNames, area=None):
        """
        """
        if area == None:
           area = self.area

        for (name, tree) in self.getXML(articleNames, area=area):
            nodes = tree.xpath('//media/uri')
            for node in nodes:
               for (a,v) in node.items():
                   yield (name, (a,v)) 

    def doiAll(self):
        """
        """
        rslt = {}
        for k, v in CURRENTS_ARTICLES.iteritems():
            rslt[k] = [ (name, doi) for (name, doi) in self.doi([ aname for aname, _ in v] , area=k)]
        return rslt
        

if __name__ == "__main__":
    import argparse   
    import pprint 
 
    # Main command dispatcher.
    dispatch = { 'get' : lambda currents, articleNames: [ etree.tostring(tree, pretty_print=True) 
                           for tree in currents.getXML(articleNames) ], 
                 'doi' : lambda currents, articleNames: [ (name, tag) for (name, tag) in currents.doi(articleNames) ],
                 'media' : lambda currents, articleNames: [ node for (name, node) in currents.media(articleNames) ],
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

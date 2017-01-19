/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

-- temporary solution to be replaced by AccMan data migration script

INSERT INTO journal (eissn, journalKey, title) VALUES ("1545-7885", "PLoSBiology",        "PLOS Biology"                    );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1549-1676", "PLoSMedicine",       "PLOS Medicine"                   );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7358", "PLoSCompBiol",       "PLOS Computational Biology"      );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7374", "PLoSPathogens",      "PLOS Pathogens"                  );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7404", "PLoSGenetics",       "PLOS Genetics"                   );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1555-5887", "PLoSClinicalTrials", "PLOS Clinical Trials"            );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1932-6203", "PLoSONE",            "PLOS ONE"                        );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1935-2735", "PLoSNTD",            "PLOS Neglected Tropical Diseases");
INSERT INTO journal (eissn, journalKey, title) VALUES ("3333-3333",        "PLoSCollections",    "PLOS Collections"         );
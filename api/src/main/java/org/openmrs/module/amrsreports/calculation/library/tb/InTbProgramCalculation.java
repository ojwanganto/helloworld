/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.amrsreports.calculation.library.tb;

import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.amrsreports.cache.MetadataUtils;
import org.openmrs.module.amrsreports.calculation.BaseEmrCalculation;
import org.openmrs.module.amrsreports.reporting.calculation.Calculations;
import org.openmrs.module.amrsreports.rule.MohEvaluableNameConstants;

import java.util.Collection;
import java.util.Map;

/**
 * Calculates whether patients are (alive and) in the TB program
 */
public class InTbProgramCalculation extends BaseEmrCalculation {

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> params, PatientCalculationContext context) {
		return passing(Calculations.activeEnrollment(MetadataUtils.getProgram(MohEvaluableNameConstants.ADMITTED_TO_HOSPITAL/*Metadata.Program.TB*/), alivePatients(cohort, context), context));
	}
}
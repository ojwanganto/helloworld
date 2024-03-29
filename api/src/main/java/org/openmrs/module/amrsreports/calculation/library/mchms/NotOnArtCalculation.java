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

package org.openmrs.module.amrsreports.calculation.library.mchms;

import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.amrsreports.cache.MetadataUtils;
import org.openmrs.module.amrsreports.calculation.BaseEmrCalculation;
import org.openmrs.module.amrsreports.calculation.EmrCalculationUtils;
import org.openmrs.module.amrsreports.reporting.calculation.BooleanResult;
import org.openmrs.module.amrsreports.reporting.calculation.CalculationUtils;
import org.openmrs.module.amrsreports.reporting.calculation.Calculations;
import org.openmrs.module.amrsreports.reporting.calculation.PatientFlagCalculation;
import org.openmrs.module.amrsreports.rule.MohEvaluableNameConstants;
import org.openmrs.module.amrsreports.util.EmrUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Calculates whether a mother is HIV+ but is not on ART. Calculation returns true if mother
 * is alive, enrolled in the MCH program, gestation is greater than 14 weeks, is HIV+ and was
 * not indicated as being on ART in the last encounter.
 */
public class NotOnArtCalculation extends BaseEmrCalculation implements PatientFlagCalculation {

	/**
	 * @see
	 */
	@Override
	public String getFlagMessage() {
		return "Not on ART";
	}

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		Program mchmsProgram = MetadataUtils.getProgram(MohEvaluableNameConstants.ADMITTED_TO_HOSPITAL/*Metadata.Program.MCHMS*/);

		Set<Integer> alive = alivePatients(cohort, context);
		Set<Integer> inMchmsProgram = CalculationUtils.patientsThatPass(Calculations.activeEnrollment(mchmsProgram, alive, context));

		CalculationResultMap lastHivStatusObss = Calculations.lastObs(getConcept(MohEvaluableNameConstants.ADMITTED_TO_HOSPITAL/*Dictionary.HIV_STATUS*/), inMchmsProgram, context);
		CalculationResultMap artStatusObss = Calculations.lastObs(getConcept(MohEvaluableNameConstants.ADMITTED_TO_HOSPITAL/*Dictionary.ANTIRETROVIRAL_USE_IN_PREGNANCY*/), inMchmsProgram, context);

		CalculationResultMap ret = new CalculationResultMap();
		for (Integer ptId : cohort) {
			boolean notOnArt = false;

			// Is patient alive and in MCH program?
			if (inMchmsProgram.contains(ptId)) {
				Concept lastHivStatus = EmrCalculationUtils.codedObsResultForPatient(lastHivStatusObss, ptId);
				Concept lastArtStatus = EmrCalculationUtils.codedObsResultForPatient(artStatusObss, ptId);
				boolean hivPositive = false;
				boolean onArt = false;
				if (lastHivStatus != null) {
					hivPositive = lastHivStatus.equals("1065"/*Dictionary.getConcept(Dictionary.POSITIVE)*/);
					if (lastArtStatus != null) {
						onArt = !lastArtStatus.equals("1066"/*Dictionary.getConcept(Dictionary.NOT_APPLICABLE)*/);
					}
				}
				notOnArt = hivPositive && gestationIsGreaterThan14Weeks(ptId) && !onArt;
			}
			ret.put(ptId, new BooleanResult(notOnArt, this, context));
		}
		return ret;
	}

	private boolean gestationIsGreaterThan14Weeks(Integer patientId) {
		Patient patient = Context.getPatientService().getPatient(patientId);
		EncounterService encounterService = Context.getEncounterService();
		EncounterType encounterType = encounterService.getEncounterTypeByUuid("1000000"/*Metadata.EncounterType.MCHMS_ENROLLMENT*/);
		Encounter lastMchEnrollment = EmrUtils.lastEncounter(patient, encounterType);
		Obs lmpObs = EmrUtils.firstObsInEncounter(lastMchEnrollment, new Concept(10)/*Dictionary.getConcept(Dictionary.LAST_MONTHLY_PERIOD)*/);
		if (lmpObs != null) {
			Weeks weeks = Weeks.weeksBetween(new DateTime(lmpObs.getValueDate()), new DateTime(new Date()));
			if (weeks.getWeeks() > 14) {
				return true;
			}
		}
		return false;
	}
}

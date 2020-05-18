package com.hartwig.hmftools.common.lims.hospital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.lims.Lims;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class HospitalModelTest {

    @Test
    public void canExtractHospitalDataFromHospitalModel() {
        HospitalModel model = buildTestHospitalModel();

        HospitalContactData dataCPCT = model.queryHospitalData("CPCT02010001T", "coreRequester", "coreRequesterEmail");
        assertEquals("CPCT-PI", dataCPCT.hospitalPI());
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataCPCT.requesterName());
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataCPCT.requesterEmail());
        assertEquals("HMF", dataCPCT.hospitalName());
        assertEquals("1000 AB AMSTERDAM", dataCPCT.hospitalAddress());

        HospitalContactData dataDRUP = model.queryHospitalData("DRUP02010001T", "coreRequester", "coreRequesterEmail");
        assertEquals("DRUP-PI", dataDRUP.hospitalPI());
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataDRUP.requesterName());
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataDRUP.requesterEmail());
        assertEquals("HMF", dataDRUP.hospitalName());
        assertEquals("1000 AB AMSTERDAM", dataDRUP.hospitalAddress());

        HospitalContactData dataWIDE = model.queryHospitalData("WIDE02010001T", "coreRequester", "coreRequesterEmail");
        assertEquals("WIDE-PI", dataWIDE.hospitalPI());
        assertEquals("WIDE-req", dataWIDE.requesterName());
        assertEquals("wide@email.com", dataWIDE.requesterEmail());
        assertEquals("HMF", dataWIDE.hospitalName());
        assertEquals("1000 AB AMSTERDAM", dataWIDE.hospitalAddress());

        HospitalContactData dataCORE = model.queryHospitalData("CORE02010001T", "coreRequester", "coreRequesterEmail");
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataCORE.hospitalPI());
        assertEquals("coreRequester", dataCORE.requesterName());
        assertEquals("coreRequesterEmail", dataCORE.requesterEmail());
        assertEquals("HMF", dataCORE.hospitalName());
        assertEquals("1000 AB AMSTERDAM", dataCORE.hospitalAddress());

        HospitalContactData dataCOREManuallyMapped = model.queryHospitalData("CORE18123456T", "coreRequester", "coreRequesterEmail");
        assertEquals(Lims.NOT_AVAILABLE_STRING, dataCOREManuallyMapped.hospitalPI());
        assertEquals("coreRequester", dataCOREManuallyMapped.requesterName());
        assertEquals("coreRequesterEmail", dataCOREManuallyMapped.requesterEmail());
        assertEquals("HMF", dataCOREManuallyMapped.hospitalName());
        assertEquals("1000 AB AMSTERDAM", dataCOREManuallyMapped.hospitalAddress());

        HospitalContactData dataSampleDoesNotExist = model.queryHospitalData("I Don't exist", "coreRequester", "coreRequesterEmail");
        assertNull(dataSampleDoesNotExist);

        HospitalContactData dataHospitalDoesNotExist = model.queryHospitalData("CPCT02020202T", "coreRequester", "coreRequesterEmail");
        assertNull(dataHospitalDoesNotExist);
    }

    @NotNull
    private static HospitalModel buildTestHospitalModel() {
        Map<String, HospitalAddress> hospitalAddress = Maps.newHashMap();
        Map<String, HospitalPersons> hospitalContactCPCT = Maps.newHashMap();
        Map<String, HospitalPersons> hospitalContactDRUP = Maps.newHashMap();
        Map<String, HospitalPersons> hospitalContactWIDE = Maps.newHashMap();
        Map<String, String> sampleHospitalMapping = Maps.newHashMap();

        hospitalAddress.put("01", ImmutableHospitalAddress.of("HMF", "1000 AB", "AMSTERDAM"));
        hospitalContactCPCT.put("01", ImmutableHospitalPersons.of("CPCT-PI", null, null));
        hospitalContactDRUP.put("01", ImmutableHospitalPersons.of("DRUP-PI", null, null));
        hospitalContactWIDE.put("01", ImmutableHospitalPersons.of("WIDE-PI", "WIDE-req", "wide@email.com"));
        sampleHospitalMapping.put("CORE18123456T", "01");

        return ImmutableHospitalModel.builder()
                .hospitalAddressMap(hospitalAddress)
                .hospitalPersonsCPCT(hospitalContactCPCT)
                .hospitalPersonsDRUP(hospitalContactDRUP)
                .hospitalPersonsWIDE(hospitalContactWIDE)
                .sampleToHospitalMapping(sampleHospitalMapping)
                .build();
    }
}

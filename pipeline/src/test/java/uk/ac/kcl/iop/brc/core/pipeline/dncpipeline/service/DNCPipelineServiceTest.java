/*
        Copyright (c) 2015 King's College London

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.ac.kcl.iop.brc.core.pipeline.common.helper.JsonHelper;
import uk.ac.kcl.iop.brc.core.pipeline.common.service.DocumentConversionService;
import uk.ac.kcl.iop.brc.core.pipeline.common.service.FileTypeService;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.DNCWorkUnitDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.PatientDao;
import uk.ac.kcl.iop.brc.core.pipeline.common.model.DNCWorkCoordinate;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.Patient;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service.anonymisation.AnonymisationService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DNCPipelineServiceTest {

    @InjectMocks
    private DNCPipelineService service;

    @Mock
    private JsonHelper<DNCWorkCoordinate> mockJsonHelper;

    @Mock
    private DNCWorkUnitDao dncWorkUnitDao;

    @Mock
    private PatientDao patientDao;

    @Mock
    private AnonymisationService anonymisationService;

    @Mock
    private DocumentConversionService documentConversionService;

    @Mock
    private FileTypeService fileTypeService;

    @Test
    public void shouldCallTextPipelineFromFileCorrectly() {
        service.setConversionFormat("text");

        List<DNCWorkCoordinate> DNCWorkCoordinates = new ArrayList<>();
        DNCWorkCoordinate cwc = new DNCWorkCoordinate();
        cwc.setPatientId(1L);
        cwc.setType("text");
        DNCWorkCoordinates.add(cwc);
        Patient patient = new Patient();
        when(mockJsonHelper.loadListFromFile(any(File.class))).thenReturn(DNCWorkCoordinates);
        when(dncWorkUnitDao.getTextFromCoordinate(any(DNCWorkCoordinate.class))).thenReturn("val");
        when(patientDao.getPatient(1L)).thenReturn(patient);
        when(anonymisationService.pseudonymisePersonPlainText(patient, "val")).thenReturn("anonymised");

        service.startCreateModeWithFile("mockFile");

        verify(dncWorkUnitDao).getTextFromCoordinate(any(DNCWorkCoordinate.class));
        verify(patientDao).getPatient(1L);
        verify(anonymisationService).pseudonymisePersonPlainText(patient, "val");
        verify(dncWorkUnitDao).saveConvertedText(cwc, "anonymised");
        verify(documentConversionService, times(0)).convertToText(any(byte[].class));
        verify(documentConversionService, times(0)).convertToXHTML(any(byte[].class));
    }

    @Test
    public void shouldCallBinaryPipelineForTextFromFileCorrectly() {
        service.setConversionFormat("text");

        List<DNCWorkCoordinate> DNCWorkCoordinates = new ArrayList<>();
        DNCWorkCoordinate cwc = new DNCWorkCoordinate();
        cwc.setPatientId(1L);
        cwc.setType("binary");
        DNCWorkCoordinates.add(cwc);
        Patient patient = new Patient();
        when(mockJsonHelper.loadListFromFile(any(File.class))).thenReturn(DNCWorkCoordinates);
        when(dncWorkUnitDao.getByteFromCoordinate(any(DNCWorkCoordinate.class))).thenReturn(new byte[1]);
        when(patientDao.getPatient(1L)).thenReturn(patient);
        when(anonymisationService.pseudonymisePersonPlainText(patient, "val")).thenReturn("anonymised");
        when(documentConversionService.convertToText(any(byte[].class))).thenReturn("val");

        service.startCreateModeWithFile("mockFile");

        verify(dncWorkUnitDao).getByteFromCoordinate(any(DNCWorkCoordinate.class));
        verify(patientDao).getPatient(1L);
        verify(anonymisationService).pseudonymisePersonPlainText(patient, "val");
        verify(dncWorkUnitDao).saveConvertedText(cwc, "anonymised");
        verify(documentConversionService, times(1)).convertToText(any(byte[].class));
    }

    @Test
    public void shouldCallBinaryPipelineForHTMLFromFileCorrectly() {
        service.setConversionFormat("html");

        List<DNCWorkCoordinate> DNCWorkCoordinates = new ArrayList<>();
        DNCWorkCoordinate cwc = new DNCWorkCoordinate();
        cwc.setPatientId(1L);
        cwc.setType("binary");
        DNCWorkCoordinates.add(cwc);
        Patient patient = new Patient();
        when(mockJsonHelper.loadListFromFile(any(File.class))).thenReturn(DNCWorkCoordinates);
        when(dncWorkUnitDao.getByteFromCoordinate(any(DNCWorkCoordinate.class))).thenReturn(new byte[1]);
        when(patientDao.getPatient(1L)).thenReturn(patient);
        when(anonymisationService.pseudonymisePersonHTML(patient, "val")).thenReturn("anonymised");
        when(documentConversionService.convertToXHTML(any(byte[].class))).thenReturn("val");

        service.startCreateModeWithFile("mockFile");

        verify(dncWorkUnitDao).getByteFromCoordinate(any(DNCWorkCoordinate.class));
        verify(patientDao).getPatient(1L);
        verify(anonymisationService).pseudonymisePersonHTML(patient, "val");
        verify(dncWorkUnitDao).saveConvertedText(cwc, "anonymised");
        verify(documentConversionService, times(1)).convertToXHTML(any(byte[].class));
    }

    @Test
    public void shouldNotSaveOCRResultTwice() throws IOException {
        service.setOcrEnabled("1");
        service.setConversionFormat("html");
        service.getCommandLineArgHolder().setNoPseudonym(true);

        DNCWorkCoordinate coordinate = new DNCWorkCoordinate();

        when(dncWorkUnitDao.getByteFromCoordinate(coordinate)).thenReturn(new byte[0]);
        when(fileTypeService.isPDF(any(byte[].class))).thenReturn(true);
        when(documentConversionService.convertToXHTML(any(byte[].class))).thenReturn("test text");

        service.processBinaryCoordinate(coordinate);

        verify(dncWorkUnitDao, times(1)).getByteFromCoordinate(coordinate);
        verify(dncWorkUnitDao, times(1)).saveConvertedText(coordinate, "test text");
        verifyZeroInteractions(dncWorkUnitDao);
    }

}

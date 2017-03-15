package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.ambraproject.rhino.util.TestReflectionUtils.setField;
import static org.ambraproject.rhombat.HttpDateUtil.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Created by jkrzemien on 7/15/14.
 */

public class AssetControllerTestWithoutSpring {

    public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private final Gson gson = new Gson();
    private final Date sampleData = new Date();
    private Calendar lastModified = Calendar.getInstance();

    @Mock
    private AssetCrudService mockAssetCrudService;

    /**
     * This could not be mocked via mockito because the methods being
     * used are PROTECTED and can't be referenced with when() definitions...this sucks
     */
    private Transceiver mockTransceiver = new Transceiver() {

        @Override
        protected Calendar getLastModifiedDate() throws IOException {
            return lastModified;
        }

        @Override
        protected Object getData() throws IOException {
            return sampleData;
        }
    };

    @InjectMocks
    private AssetController assetController;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        reset(mockAssetCrudService);

        /**
         * Due to bad class design, you need to use either:
         * - Spring context to auto wire fields
         * - Reflection to set invisible fields
         *
         * And due to Mockito limitations:
         * - Can't mock final classes (Gson)
         *
         * And since for this example I don't want to involve Spring containers for unit tests...
         */
        setField(assetController, "entityGson", gson);

        /**
         * Set up stand alone Asset Controller and define minimal amount of expectations for all tests
         */
        this.mockMvc = standaloneSetup(assetController)
                .alwaysDo(print())
                .alwaysExpect(status().isOk())
                .alwaysExpect(header().string("Content-Type", CONTENT_TYPE_JSON))
                .alwaysExpect(content().contentType(CONTENT_TYPE_JSON))
                .alwaysExpect(header().string("Last-Modified", format(lastModified)))
                .alwaysExpect(forwardedUrl(null))
                .alwaysExpect(redirectedUrl(null))
                .build();
    }

    @Test
    public void readTest() throws Exception {
        String assetIdRestParamValue = "1234";
        AssetIdentity assetIdentity = AssetIdentity.create(assetIdRestParamValue);

        when(mockAssetCrudService.readMetadata(assetIdentity)).thenReturn(mockTransceiver);

        MvcResult result = mockMvc.perform(get("/assets/{assetId}", assetIdRestParamValue)).andReturn();

        assertNotNull(result);
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(gson.toJson(sampleData), response.getContentAsString());

        verify(mockAssetCrudService).readMetadata(assetIdentity);
        verifyNoMoreInteractions(mockAssetCrudService);
    }

    @Test
    public void readAsFigureTest() throws Exception {
        String figureParamValue = "this_param_is_being_totally_ignored!";   // Bug?
        String assetIdRestParamValue = "this_should_not_be_required!";      // Bug?

        AssetIdentity assetIdentity = AssetIdentity.create(assetIdRestParamValue);

        when(mockAssetCrudService.readFigureMetadata(eq(assetIdentity))).thenReturn(mockTransceiver);

        MvcResult result = mockMvc.perform(
                get("/assets/{this_in_non_sense}", assetIdRestParamValue)
                        .param("figure", figureParamValue)
        ).andReturn();

        assertNotNull(result);
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(gson.toJson(sampleData), response.getContentAsString());

        verify(mockAssetCrudService).readFigureMetadata(assetIdentity);
        verifyNoMoreInteractions(mockAssetCrudService);
    }

}

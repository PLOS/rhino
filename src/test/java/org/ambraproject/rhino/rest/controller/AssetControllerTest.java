package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.ambraproject.rhombat.HttpDateUtil.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


/**
 * Created by jkrzemien on 7/15/14.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class AssetControllerTest {

    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private static final Date SAMPLE_DATA = new Date();
    private static final Calendar LAST_MODIFIED = Calendar.getInstance();

    private MockMvc mockMvc;
    private final Gson gson = new Gson();

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    private AssetCrudService mockAssetCrudService;

    @Autowired
    private Transceiver mockTransceiver;

    @Autowired
    private AssetController assetController;

    @Before
    public void setUp() throws IOException {
        this.mockMvc = webAppContextSetup(context)
                .alwaysDo(print())
                .alwaysExpect(handler().handlerType(AssetController.class))
                .alwaysExpect(status().isOk())
                .alwaysExpect(header().string("Content-Type", CONTENT_TYPE_JSON))
                .alwaysExpect(header().string("Last-Modified", format(LAST_MODIFIED)))
                .alwaysExpect(content().contentType(CONTENT_TYPE_JSON))
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
        assertEquals(gson.toJson(SAMPLE_DATA), response.getContentAsString());

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
        assertEquals(gson.toJson(SAMPLE_DATA), response.getContentAsString());

        verify(mockAssetCrudService).readFigureMetadata(assetIdentity);
        verifyNoMoreInteractions(mockAssetCrudService);
    }

    /**
     * Spring container configuration for this test.
     * I don't like having multiple XML Spring files (per test) nor having a single huge one for all tests.
     * I think it communicates better if each test suite has it's own Spring config embedded in it as Java code.
     */
    @Configuration
    static class TestConfiguration {

        @Bean
        public AssetController getClassUnderTest() {
            return new AssetController();
        }

        @Bean
        public AssetCrudService getAssetCrudServiceDependency() {
            return mock(AssetCrudService.class);
        }

        /**
         * This could not be mocked via mockito because the methods being
         * used are PROTECTED and can't be referenced with when() definitions...this sucks
         */
        @Bean
        public Transceiver getTransceiverDependency() {
            return new Transceiver() {
                @Override
                protected Calendar getLastModifiedDate() throws IOException {
                    return LAST_MODIFIED;
                }

                @Override
                protected Object getData() throws IOException {
                    return SAMPLE_DATA;
                }
            };
        }

        /**
         * Can't mock final classes (Gson) with Mockito. But since it is "lightweight"
         * this is not much of a pain...otherwise, should be mocked out with some other
         * mocking framework (PowerMock for ex.) or with an actual mock/stub class.
         */
        @Bean
        public Gson getGsonDependency() {
            return new Gson();
        }

    }
}

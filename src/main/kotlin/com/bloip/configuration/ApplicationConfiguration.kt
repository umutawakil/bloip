package com.bloip.configuration

import org.springframework.boot.web.server.MimeMappings
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Configuration

/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration : WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    override fun customize(factory: ConfigurableServletWebServerFactory?) {
        val mappings =  MimeMappings(MimeMappings.DEFAULT)
        mappings.add("wasm", "application/wasm")
        factory!!.setMimeMappings(mappings);
    }
}

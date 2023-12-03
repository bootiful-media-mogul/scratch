package com.joshlong.mogul.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@Controller
@ResponseBody
class IndexController {

	@GetMapping("/")
	Map<String, String> hello(Principal principal) {
		return Map.of("message", "Hello " + principal.getName());
	}

}

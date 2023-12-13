package com.joshlong.mogul.api.managedfiles;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
class ManagedFileController {

	private final ManagedFileService managedFileService;

	private final MogulService mogulService;

	ManagedFileController(ManagedFileService managedFileService, MogulService mogulService) {
		this.managedFileService = managedFileService;
		this.mogulService = mogulService;
	}

	@QueryMapping
	ManagedFile managedFileById(@Argument Long id ) {
		return this.managedFileService.getManagedFile( id );
	}

	@ResponseBody
	@PostMapping("/managedfiles/{id}")
	Map<String, Number> write(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file)
			throws Exception {
		Assert.notNull(id, "the id should not be null");
		var mogul = this.mogulService.getCurrentMogul();
		var managedFile = this.managedFileService.getManagedFile(id);
		Assert.notNull(managedFile, "the managedfile is null for managed file id [" + id + "]");
		Assert.state(managedFile.mogulId().equals(mogul.id()), "you're trying to write to an invalid file " +
				"to which you are not authorized!");
		this.managedFileService.write(managedFile.id(), file.getResource());
		return Map.of("managedFileId", id);
	}

}

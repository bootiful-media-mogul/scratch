package com.joshlong.mogul.api.managedfiles;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

@Controller
class ManagedFileController {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
	Map<String, Object> write(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
		Assert.notNull(id, "the id should not be null");
		var mogul = this.mogulService.getCurrentMogul();
		var managedFile = this.managedFileService.getManagedFile(id);
		Assert.notNull(managedFile, "the managed file is null for managed file id [" + id + "]");
		Assert.state(managedFile.mogulId().equals(mogul.id()), "you're trying to write to an invalid file to which you are not authorized!");
		var originalFilename = file.getOriginalFilename();
		this.managedFileService.write(managedFile.id(), originalFilename, file.getResource());
		var updated = managedFileService.getManagedFile( managedFile.id()) ;
		log.debug("finished writing managed file [" + id + "] to s3: " + originalFilename + ":" +
				updated.toString());
		return Map.of("managedFileId", id, "uid", UUID.randomUUID().toString());
	}

}

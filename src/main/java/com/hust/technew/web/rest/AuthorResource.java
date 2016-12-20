package com.hust.technew.web.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;
import com.hust.technew.domain.Author;
import com.hust.technew.domain.Authority;
import com.hust.technew.domain.User;
import com.hust.technew.repository.AuthorRepository;
import com.hust.technew.repository.AuthorityRepository;
import com.hust.technew.repository.UserRepository;
import com.hust.technew.security.AuthoritiesConstants;
import com.hust.technew.service.AuthorService;
import com.hust.technew.service.StorageService;
import com.hust.technew.service.UserService;
import com.hust.technew.service.dto.AuthorDTO;
import com.hust.technew.service.dto.AvatarDTO;
import com.hust.technew.service.mapper.AuthorMapper;
import com.hust.technew.web.rest.util.HeaderUtil;
import com.hust.technew.web.rest.util.PaginationUtil;

/**
 * REST controller for managing Author.
 */
@RestController
@RequestMapping("/api")
public class AuthorResource {

	private final Logger log = LoggerFactory.getLogger(AuthorResource.class);

	@Inject
	private AuthorRepository authorRepository;

	@Inject
	private AuthorMapper authorMapper;

	@Inject
	private AuthorService authorService;
	
	@Inject
	private StorageService storageService;
	
	@Inject
	private UserService userService;
	
	@Inject
	private AuthorityRepository authorityRepository;
	
	@Inject
	private UserRepository userRepository;

	/**
	 * POST /authors : Create a new author.
	 *
	 * @param authorDTO
	 *            the authorDTO to create
	 * @return the ResponseEntity with status 201 (Created) and with body the
	 *         new authorDTO, or with status 400 (Bad Request) if the author has
	 *         already an ID
	 * @throws URISyntaxException
	 *             if the Location URI syntax is incorrect
	 */
	@RequestMapping(value = "/authors", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured(AuthoritiesConstants.ADMIN)
	public ResponseEntity<AuthorDTO> createAuthor(@Valid @RequestBody AuthorDTO authorDTO) throws URISyntaxException {
		log.debug("REST request to save Author : {}", authorDTO);
		if (authorDTO.getId() != null) {
			return ResponseEntity.badRequest().headers(
					HeaderUtil.createFailureAlert("author", "idexists", "A new author cannot already have an ID"))
					.body(null);
		}
		Author author = authorMapper.authorDTOToAuthor(authorDTO);
		author = authorRepository.save(author);
		User user = userService.getUserWithAuthorities(authorDTO.getUserId());
		Set<Authority> authorities = user.getAuthorities();
		authorities.add(authorityRepository.findOne(AuthoritiesConstants.AUTHOR));
		user.setAuthorities(authorities);
		userRepository.save(user);
		AuthorDTO result = authorMapper.authorToAuthorDTO(author);
		return ResponseEntity.created(new URI("/api/authors/" + result.getId()))
				.headers(HeaderUtil.createEntityCreationAlert("author", result.getId().toString())).body(result);
	}

	/**
	 * PUT /authors : Updates an existing author.
	 *
	 * @param authorDTO
	 *            the authorDTO to update
	 * @return the ResponseEntity with status 200 (OK) and with body the updated
	 *         authorDTO, or with status 400 (Bad Request) if the authorDTO is
	 *         not valid, or with status 500 (Internal Server Error) if the
	 *         authorDTO couldnt be updated
	 * @throws URISyntaxException
	 *             if the Location URI syntax is incorrect
	 */
	@RequestMapping(value = "/authors", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured({ AuthoritiesConstants.AUTHOR, AuthoritiesConstants.ADMIN})
	public ResponseEntity<AuthorDTO> updateAuthor(@Valid @RequestBody AuthorDTO authorDTO) throws URISyntaxException {
		log.debug("REST request to update Author : {}", authorDTO);
		if (authorDTO.getId() == null) {
			return createAuthor(authorDTO);
		}
		Author author = authorRepository.findOne(authorDTO.getId());
		if (userService.hasAuthority(AuthoritiesConstants.ADMIN))
			authorMapper.updateByAdmin(authorDTO, author);
		else
			authorMapper.updateByAuthor(authorDTO, author);
		author = authorRepository.save(author);
		AuthorDTO result = authorMapper.authorToAuthorDTO(author);
		return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert("author", authorDTO.getId().toString()))
				.body(result);
	}
	
	/**
	 * PUT /authors/:id/avatar : update avatar
	 * 
	 */
	@RequestMapping(value = "/authors/{id}/avatar", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured(AuthoritiesConstants.AUTHOR)
	public ResponseEntity<AvatarDTO> updateAvatar(@RequestParam MultipartFile file) {
		try {
			String path = authorService.changeAvatar(file);
			return new ResponseEntity<>(new AvatarDTO(path), HttpStatus.OK);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * GET /authors/{id}/avatar/{name} : GET avatar of author
	 * 
	 */
	@RequestMapping(value = "/authors/{id}/avatar/{size}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<Resource> getAvatar(@PathVariable Long id, @PathVariable String size) {
		String path = "/authors/" + id + "/avatar/" + size;
		try {
			Resource avatar = storageService.loadResource(path);
			return ResponseEntity.ok().body(avatar);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * GET /authors : get all the authors.
	 *
	 * @param pageable
	 *            the pagination information
	 * @return the ResponseEntity with status 200 (OK) and the list of authors
	 *         in body
	 * @throws URISyntaxException
	 *             if there is an error to generate the pagination HTTP headers
	 */
	@RequestMapping(value = "/authors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured(AuthoritiesConstants.ADMIN)
	public ResponseEntity<List<AuthorDTO>> getAllAuthors(Pageable pageable) throws URISyntaxException {
		log.debug("REST request to get a page of Authors");
		Page<Author> page = authorRepository.findAll(pageable);
		HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/authors");
		return new ResponseEntity<>(authorMapper.authorsToAuthorDTOs(page.getContent()), headers, HttpStatus.OK);
	}

	/**
	 * GET /authors/:id : get the "id" author.
	 *
	 * @param id
	 *            the id of the authorDTO to retrieve
	 * @return the ResponseEntity with status 200 (OK) and with body the
	 *         authorDTO, or with status 404 (Not Found)
	 */
	@RequestMapping(value = "/authors/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<AuthorDTO> getAuthor(@PathVariable Long id) {
		log.debug("REST request to get Author : {}", id);
		Author author = authorRepository.findOne(id);
		AuthorDTO authorDTO = authorMapper.authorToAuthorDTO(author);
		return Optional.ofNullable(authorDTO).map(result -> new ResponseEntity<>(result, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	/**
	 * GET /authors/current : get current author.
	 *
	 * @return the ResponseEntity with status 200 (OK) and with body the
	 *         memberDTO, or with status 404 (Not Found)
	 */
	@RequestMapping(value = "/authors/current", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured(AuthoritiesConstants.AUTHOR)
	public ResponseEntity<AuthorDTO> getCurrentAuthor() {
		log.debug("REST request to get current Author : {}");
		Author author = authorService.getCurrentAuthor();
		AuthorDTO authorDTO = authorMapper.authorToAuthorDTO(author);
		return Optional.ofNullable(authorDTO).map(result -> new ResponseEntity<>(result, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	/**
	 * DELETE /authors/:id : delete the "id" author.
	 *
	 * @param id
	 *            the id of the authorDTO to delete
	 * @return the ResponseEntity with status 200 (OK)
	 */
	@RequestMapping(value = "/authors/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@Secured(AuthoritiesConstants.ADMIN)
	public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
		log.debug("REST request to delete Author : {}", id);
		authorRepository.delete(id);
		return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("author", id.toString())).build();
	}

}

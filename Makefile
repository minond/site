SERVER_PORT ?= 8081
FSPRESS_FLAGS = -post-glob 'posts/[0-9]*.md' -post-template posts/post.tmpl -post-catalog posts/catalog.csv

compile: docs/Marcos-Minond-Resume.pdf images
	fspressc $(FSPRESS_FLAGS) -out posts
	$(MAKE) minify

minify:
	# https://www.npmjs.com/package/html-minifier
	for file in `ls posts/*.html`; do \
		html-minifier --collapse-whitespace $$file -o $$file ; \
	done

images: posts/1531094894-adt-type-meaning.svg

posts/1531094894-adt-type-meaning.svg: posts/1531094894-adt-type-meaning.mmd
	mmdc -i posts/1531094894-adt-type-meaning.mmd -o posts/1531094894-adt-type-meaning.svg

server:
	fspress $(FSPRESS_FLAGS) -dev -listen ":$(SERVER_PORT)"

docs/Marcos-Minond-Resume.pdf: resume.tex
	xelatex -output-format=pdf resume.tex
	mv resume.pdf docs/Marcos-Minond-Resume.pdf
	$(MAKE) clean

clean:
	rm resume.aux resume.log resume.out

dependencies:
	npm i -g html-minifier
	go install github.com/minond/fspress/cmd/fspress@latest
	go install github.com/minond/fspress/cmd/fspressc@latest

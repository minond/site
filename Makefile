SERVER_PORT ?= 8081
FSPRESS_FLAGS = -glob 'posts/[0-9]*.md' -post-template posts/post.tmpl -catalog posts/catalog.csv

compile: docs/Marcos-Minond-Resume.pdf posts/1531094894-adt-type-meaning.mmd
	fspressc $(FSPRESS_FLAGS) -out posts

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

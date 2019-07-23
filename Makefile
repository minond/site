SERVER_PORT ?= 8081
FSPRESS_FLAGS = -glob 'posts/[0-9]*.md' -post-template posts/post.tmpl -catalog posts/catalog.csv

compile: docs/Marcos-Minond-Resume.pdf
	fspressc $(FSPRESS_FLAGS) -out posts

server:
	fspress $(FSPRESS_FLAGS) -dev -listen ":$(SERVER_PORT)"

docs/Marcos-Minond-Resume.pdf: resume.tex
	xelatex -output-format=pdf resume.tex
	mv resume.pdf docs/Marcos-Minond-Resume.pdf
	$(MAKE) clean

clean:
	rm resume.aux resume.log resume.out

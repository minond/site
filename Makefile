.PHONY: resume.tex

resume.tex:
	xelatex -output-format=pdf resume.tex
	mv resume.pdf Marcos-Minond-Resume.pdf
	$(MAKE) clean

clean:
	rm resume.aux resume.log resume.out

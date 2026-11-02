FROM python:3.10-alpine

WORKDIR /

COPY client/ /python-client/

# Build python client, install locally
WORKDIR /python-client/
RUN pip install .
RUN pip install -r requirements.txt
RUN pip install -r test-requirements.txt

COPY . /python/
WORKDIR /python/

CMD ["python", "test/python_test.py"]
package com.structurizr.analysis.myapp.service;

import com.structurizr.analysis.myapp.data.SomeOtherRepository;
import com.structurizr.analysis.myapp.data.SomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SomeServiceImpl implements SomeService {

    @Autowired
    private SomeRepository someRepository;

    @Autowired
    private SomeOtherRepository someOtherRepository;

    public void doSomething() {
        someRepository.findSomething();
    }

}

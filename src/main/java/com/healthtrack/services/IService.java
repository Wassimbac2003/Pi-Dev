package com.healthtrack.services;

import java.util.List;

public interface IService<T> {
    void ajouter(T t);

    void supprimer(int id);

    void modifier(T t);

    List<T> getAll();

    T getOneById(int id);
}

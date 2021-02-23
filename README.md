## **KoaNdeAire**
Es una aplicación para dispositivos móviles con sistema operativo Android, desarrollado para el proyecto [Aire Libre](http://airelib.re/ "Aire Libre").
El aplicativo tiene como objetivo triangular la ubicación actual (latitud, longitud y ciudad) y en base a esos datos determinar el sensor más cercano a ese punto, de forma que se pueda visualizar únicamente información proveídas por los mismos.
Dicha aplicación dispone de un widget para que se pueda observar los datos desde la pantalla de inicio del móvil y también para que se actualice cada cierto tiempo (en este caso se definió cada 30 minutos que es el tiempo mínimo de actualización que permite Android).
Está escrito en el lenguaje programación Java con el entorno de desarrollo Android Studio.


**Funcionamiento**
- Determina la ubicación geográfica utilizando el sensor del GPS, posteriormente con la herramienta para buscar datos [nominatim](https://nominatim.org/ "nominatim") de OpenStreetMap realiza codificación geográfica inversa para determina la ciudad a la que pertenece las coordenadas proveídas por el GPS.
- Utilizando un scraper, se extrae de la página [airelib.re](http://airelib.re/ "airelib.re") la ubicación de los sensores para determinar cual es el más cercano a la posición actual y de acuerdo a eso extraer las demás informaciones (como el AQI y demás). Luego consulta la hora de actualización y se envía a los TextView correspondientes para su visualización.
- En el caso de que se haya generado el widget las informaciones se actualizan automáticamente cada cierto tiempo, también se puede actualizar de forma manual pulsando el TextView (específicamente que muestra la hora y fecha de actualización) que se encuentra en el widget.

**Demo**
- Funcionamiento de la aplicación (los enlaces redirigen a YouTube).

[![Demostracion de la app](https://img.youtube.com/vi/k1bmTfZA_t4/mqdefault.jpg)](https://www.youtube.com/watch?v=k1bmTfZA_t4)

- Actualización de la información desde el widget.

[![Demostracion del widget](https://img.youtube.com/vi/jYrPptpyHyM/mqdefault.jpg)](https://www.youtube.com/watch?v=jYrPptpyHyM)

**Ejecutar**
- Para el correcto funcionamiento de la app modifique los siguientes archivos:
 - scrapear_airebot.py (línea 1)
 - CallProcesos.java (línea 55)

**Observación**
- Para determinar la ubicación tarda bastante tiempo ya que solo hace uso del GPS y no de algún tipo de proveedor de mapas.
- No se definió ningún determinado rango de distancia máxima para que se siga pudiendo consultar la información de un sensor, por lo tanto, estando en cualquier parte del país (como es mi caso) e inclusive del mundo la app va a seguir consultando a los sensores como se puede observar en las siguientes imagenes.

![Figura 1](https://drive.google.com/uc?export=view&id=12ktFTbmpUviymfdz39_CuQ3CmsIGC_7R
) ![Figura 2](https://drive.google.com/uc?export=view&id=1NNO5Cf0Qs91wJwo7_tjXSflkzp86FTdk)

- Para el scraping el algoritmo está escrito en Python (por cierto, que creo es ultra básico jaja).
- Para finalizar no estoy muy seguro de que el término "scraper, escrapear" exista o se esté utilizando jaja.
